package dev.createautoflight.content.gyroscope;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelReactionWheel;
import dev.ryanhcode.sable.api.block.BlockSubLevelDynamicCollider;
import dev.ryanhcode.sable.api.physics.collider.VoxelColliderData;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createautoflight.content.navigation.AssemblyResolver;
import dev.createautoflight.content.navigation.AutoflightAssemblyBlock;
import dev.createautoflight.content.navigation.ColliderShrinkHelper;
import dev.createautoflight.content.navigation.FlightCommand;
import dev.createautoflight.content.navigation.FlightCommandBus;
import dev.createautoflight.content.navigation.GyroNavController;
import dev.createautoflight.content.navigation.GyroTargetAngles;
import dev.createautoflight.content.navigation.NavigationKinematics;
import dev.createautoflight.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;

public class GyroscopeBlockEntity extends SmartBlockEntity
        implements BlockEntitySubLevelReactionWheel, BlockEntitySubLevelActor,
        BlockSubLevelDynamicCollider, AutoflightAssemblyBlock {

    /** Per-gyro torque cap — constant regardless of contraption size; stack gyros for more effect. */
    public static final double BASE_TORQUE = 50_000.0;
    public static final int MAX_DAMPING_PERCENT = 90;
    private static final double AUTO_KP = 2.4;
    private static final double STRENGTH_SCALE = 0.1;
    /** Full correction until accept angle; tiny value = no fade-out while still outside threshold. */
    private static final double CORRECTION_RAMP = 1e-4;
    private static final double SETTLE_ANG_VEL = 0.05;
    /** Visual ring spin multiplier — decoupled from physics for readability */
    private static final double DISPLAY_SCALE = 30.0;

    public enum GyroMode {
        AUTO,
        MANUAL
    }

    private GyroMode mode = GyroMode.AUTO;
    private boolean autoStabilize = true;
    private int forcePercent = 100;
    private int dampingPercent = 50;
    private int acceptAngleDeg = 3;
    private boolean stabilizePitch = true;
    private boolean stabilizeYaw = true;
    private boolean stabilizeRoll = true;
    private boolean bidirectionalTorque = true;
    private Direction downFace = Direction.DOWN;
    private int targetPitchDeg;
    private int targetYawDeg;
    private int targetRollDeg;
    /** True while navigation heli mode is broadcasting attitude targets to this gyro. */
    private boolean navTargetOverride;

    /** Internal wheel momentum — limits one-way mode; visual spin in both modes. */
    private final Vector3d wheelMomentum = new Vector3d();

    /** Synced to client for ring animation. */
    private final Vector3d targetAngularVelocity = new Vector3d();
    private final Vector3d displayAngles = new Vector3d();

    private boolean litNorth;
    private boolean litEast;
    private boolean litSouth;
    private boolean litWest;

    private int signalNorth;
    private int signalEast;
    private int signalSouth;
    private int signalWest;

    private int syncTicks;

    private final GyroPrecisionController precisionController = new GyroPrecisionController();
    private final GyroPrecisionController navPrecisionController = new GyroPrecisionController();

    public GyroscopeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GYROSCOPE.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours) {
        // Settings are configured through GyroscopeScreen on block use.
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Direction placedDown = GyroscopeBlock.consumePlacementDown(worldPosition);
            if (placedDown != null) {
                downFace = placedDown;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            if (targetAngularVelocity.lengthSquared() > 1e-6 && ++syncTicks % 40 == 0) {
                setChanged();
            }
        }
    }

    private void tickClientAnimation() {
        double dt = 1.0 / 20.0;
        displayAngles.x += targetAngularVelocity.x * dt * DISPLAY_SCALE;
        displayAngles.y += targetAngularVelocity.y * dt * DISPLAY_SCALE;
        displayAngles.z += targetAngularVelocity.z * dt * DISPLAY_SCALE;
    }

    public void onNeighborChanged() {
        if (level == null || level.isClientSide) {
            return;
        }
        readRedstone();
        setChanged();
    }

    private void readRedstone() {
        assert level != null;
        BlockPos p = worldPosition;
        signalNorth = level.getSignal(p.relative(Direction.NORTH), Direction.NORTH);
        signalEast = level.getSignal(p.relative(Direction.EAST), Direction.EAST);
        signalSouth = level.getSignal(p.relative(Direction.SOUTH), Direction.SOUTH);
        signalWest = level.getSignal(p.relative(Direction.WEST), Direction.WEST);

        litNorth = signalNorth > 0;
        litEast = signalEast > 0;
        litSouth = signalSouth > 0;
        litWest = signalWest > 0;
    }

    /** Damping strength is independent of the force slider — resistance-only mode uses this alone. */
    private double computeMaxCorrectionTorque() {
        return BASE_TORQUE * (forcePercent / 100.0) * STRENGTH_SCALE;
    }

    private Vector3d computeManualTorque(double maxTorque) {
        Vector3d torque = new Vector3d();
        if (stabilizePitch) {
            torque.x += (signalNorth - signalSouth) / 15.0 * maxTorque;
        }
        if (stabilizeYaw) {
            torque.y += (signalEast - signalWest) / 15.0 * maxTorque;
        }
        if (stabilizeRoll) {
            torque.z += (signalEast + signalWest - signalNorth - signalSouth) / 60.0 * maxTorque;
        }
        return torque;
    }

    private void applyTorqueAtGyro(ServerSubLevel subLevel, Vector3d localTorque, double maxTorque, double dt) {
        if (localTorque.lengthSquared() < 1e-12) {
            targetAngularVelocity.mul(0.85);
            setChanged();
            return;
        }

        Vector3d torque = new Vector3d(localTorque);
        if (!bidirectionalTorque) {
            torque = enforceOneWayTorque(torque, maxTorque);
        }

        Vector3d impulse = new Vector3d(torque).mul(dt);
        clampVector(impulse, maxTorque * dt);

        double wheelScale = maxTorque * dt;
        wheelMomentum.add(impulse.x / wheelScale, impulse.y / wheelScale, impulse.z / wheelScale);
        clampVector(wheelMomentum, 1.0);

        subLevel.getOrCreateQueuedForceGroup(
                ForceGroups.REGISTRY.get(ResourceLocation.fromNamespaceAndPath("sable", "propulsion"))
        ).getForceTotal().applyTorqueImpulse(impulse);

        syncDisplayFromWheel();
        setChanged();
    }

    private Vector3d enforceOneWayTorque(Vector3d torque, double maxTorque) {
        Vector3d allowed = new Vector3d(torque);
        double cap = maxTorque * 0.5;
        for (int i = 0; i < 3; i++) {
            double t = torque.get(i);
            double w = wheelMomentum.get(i) * cap;
            if (Math.abs(t) < 1e-9) {
                continue;
            }
            if (t * w < 0) {
                continue;
            }
            if (t > 0 && w >= cap) {
                allowed.setComponent(i, 0);
            } else if (t < 0 && w <= -cap) {
                allowed.setComponent(i, 0);
            }
        }
        return allowed;
    }

    private void syncDisplayFromWheel() {
        targetAngularVelocity.set(wheelMomentum);
    }

    private static void clampVector(Vector3d v, double max) {
        double len = v.length();
        if (len > max && len > 1e-6) {
            v.mul(max / len);
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (level == null || level.isClientSide) {
            return;
        }
        readRedstone();
        double maxCorrection = computeMaxCorrectionTorque();
        Quaterniond orientation = subLevel.logicalPose().orientation();
        Vector3d localAngVel = orientation.transformInverse(body.getAngularVelocity(new Vector3d()), new Vector3d());

        ServerSubLevel root = AssemblyResolver.resolveRootAssembly(subLevel);
        FlightCommand navCommand = FlightCommandBus.get(root.getUniqueId());
        GyroTargetAngles eulerTarget = resolveEulerTarget(navCommand);
        Quaterniond qTarget = resolveTargetOrientation(orientation, navCommand, eulerTarget);

        int effDamping = dampingPercent;
        double forceMult = 1.0;
        Vector3d correctionTorque = null;

        if (forcePercent > 0) {
            if (mode == GyroMode.MANUAL) {
                correctionTorque = computeManualTorque(maxCorrection);
            } else if (autoStabilize) {
                GyroNavController.GyroNavTorque navTorque = GyroNavController.computeTowardOrientation(
                        orientation,
                        qTarget,
                        localAngVel,
                        stabilizePitch,
                        stabilizeYaw,
                        stabilizeRoll,
                        acceptAngleDeg
                );

                boolean zeroTolerance = acceptAngleDeg == 0;
                GyroPrecisionController.PrecisionState precision = zeroTolerance
                        ? navPrecisionController.update(navTorque.headingErrorRad())
                        : GyroPrecisionController.PrecisionState.inactive();

                if (zeroTolerance && navTorque.headingErrorRad() > GyroPrecisionController.PRECISION_ZONE_RAD) {
                    navPrecisionController.reset();
                } else if (!zeroTolerance) {
                    navPrecisionController.reset();
                }

                if (precision.active()) {
                    effDamping = precision.dampingPercent();
                    forceMult = precision.forceMultiplier();
                }

                if (navTorque.localTorque().lengthSquared() > 1e-12) {
                    correctionTorque = new Vector3d(navTorque.localTorque()).mul(forceMult * maxCorrection);
                }
            }
            if (correctionTorque != null && correctionTorque.lengthSquared() > 1e-12) {
                applyTorqueAtGyro(subLevel, correctionTorque, maxCorrection * forceMult, dt);
            }
        } else {
            navPrecisionController.reset();
        }

        applyRotationalDamping(body, orientation, effDamping);
    }

    private GyroTargetAngles resolveEulerTarget(FlightCommand navCommand) {
        if (navCommand.navActive()) {
            GyroTargetAngles nav = navCommand.gyroTargetAngles();
            syncNavTargetsToDisplay(nav);
            if (!navTargetOverride) {
                navTargetOverride = true;
                setChanged();
            }
            return nav;
        }
        if (navTargetOverride) {
            navTargetOverride = false;
            setChanged();
        }
        return new GyroTargetAngles(targetPitchDeg, targetYawDeg, targetRollDeg);
    }

    private void syncNavTargetsToDisplay(GyroTargetAngles nav) {
        int p = (int) Math.round(Math.clamp(nav.pitchDeg(), -90, 90));
        int y = (int) Math.round(wrapDegrees(nav.yawDeg()));
        int r = (int) Math.round(Math.clamp(nav.rollDeg(), -90, 90));
        if (p != targetPitchDeg || y != targetYawDeg || r != targetRollDeg) {
            targetPitchDeg = p;
            targetYawDeg = y;
            targetRollDeg = r;
            setChanged();
        }
    }

    private Quaterniond resolveTargetOrientation(
            Quaterniond currentOrientation,
            FlightCommand navCommand,
            GyroTargetAngles eulerTarget
    ) {
        if (navCommand.navActive()) {
            // Follow the navigator's desired orientation directly in every nav mode. This field
            // carries the full target attitude; in helicopter mode it equals the level-attitude
            // orientation built from the gyro targets, so heli behavior is preserved.
            return new Quaterniond(navCommand.desiredOrientation());
        }
        if (eulerTarget.pitchDeg() == 0.0
                && eulerTarget.yawDeg() == 0.0
                && eulerTarget.rollDeg() == 0.0) {
            return levelDownFaceOrientation(currentOrientation);
        }
        return NavigationKinematics.orientationFromLevelAttitude(eulerTarget);
    }

    /** Levels the configured down face toward world gravity while preserving heading. */
    private Quaterniond levelDownFaceOrientation(Quaterniond currentOrientation) {
        Vector3d localDown = localDownFaceDirection();
        Vector3d currentDown = currentOrientation.transform(localDown, new Vector3d()).normalize();
        Vector3d worldDown = new Vector3d(0, -1, 0);
        if (currentDown.dot(worldDown) > 0.9999) {
            return new Quaterniond(currentOrientation);
        }
        Quaterniond qDelta = GyroNavController.rotationBetween(currentDown, worldDown);
        return qDelta.mul(currentOrientation, new Quaterniond());
    }

    private static double wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0;
        if (wrapped > 180.0) {
            wrapped -= 360.0;
        } else if (wrapped < -180.0) {
            wrapped += 360.0;
        }
        return wrapped;
    }

    @Override
    public void buildBoxes(VoxelColliderData data) {
        if (level == null) {
            return;
        }
        var containing = dev.ryanhcode.sable.Sable.HELPER.getContaining(level, worldPosition);
        if (containing instanceof ServerSubLevel subLevel) {
            ColliderShrinkHelper.buildBoxes(data, AssemblyResolver.resolveRootAssembly(subLevel), worldPosition, false);
        }
    }

    private Vector3d localDownFaceDirection() {
        return switch (downFace) {
            case DOWN -> new Vector3d(0, -1, 0);
            case UP -> new Vector3d(0, 1, 0);
            case NORTH -> new Vector3d(0, 0, -1);
            case SOUTH -> new Vector3d(0, 0, 1);
            case WEST -> new Vector3d(-1, 0, 0);
            case EAST -> new Vector3d(1, 0, 0);
        };
    }

    private static double alignmentAngle(Vector3d currentDir, Vector3d targetDir) {
        double cos = Math.clamp(currentDir.dot(targetDir), -1.0, 1.0);
        double sin = currentDir.cross(targetDir, new Vector3d()).length();
        return Math.atan2(sin, cos);
    }

    private static Vector3d computeLocalCorrection(
            Quaterniond orientation,
            Vector3d currentDownWorld,
            Vector3d targetDownWorld,
            double angle
    ) {
        if (angle < 1e-6) {
            return new Vector3d();
        }
        Vector3d axisWorld = currentDownWorld.cross(targetDownWorld, new Vector3d());
        if (axisWorld.lengthSquared() < 1e-10) {
            return new Vector3d();
        }
        axisWorld.normalize();
        return orientation.transformInverse(axisWorld, new Vector3d()).mul(angle);
    }

    /** 0 inside accept zone, ramps to 1 beyond it. */
    private static double correctionScaleFromAngle(double angleRad, double acceptRad) {
        if (angleRad <= acceptRad) {
            return 0.0;
        }
        return Math.clamp((angleRad - acceptRad) / CORRECTION_RAMP, 0.0, 1.0);
    }

    private static double correctionTorque(double correction, double angleScale) {
        return Math.clamp(correction * AUTO_KP * angleScale, -1.0, 1.0);
    }

    /**
     * Removes a fraction of current spin each tick. 100% would zero all rotation; 90% slider retains 10%.
     * Applied after correction so it always wins over stabilization torque.
     */
    private void applyRotationalDamping(RigidBodyHandle body, Quaterniond orientation) {
        applyRotationalDamping(body, orientation, dampingPercent);
    }

    private void applyRotationalDamping(RigidBodyHandle body, Quaterniond orientation, int dampingOverride) {
        int activeDamping = dampingOverride >= 0 ? dampingOverride : dampingPercent;
        if (activeDamping <= 0) {
            return;
        }

        // 90% slider = full spin lock on enabled axes; scales linearly below that.
        double spinRetention = 1.0 - ((double) activeDamping / MAX_DAMPING_PERCENT);

        Vector3d worldOmega = body.getAngularVelocity(new Vector3d());
        Vector3d localOmega = orientation.transformInverse(worldOmega, new Vector3d());
        Vector3d newLocal = new Vector3d(localOmega);

        if (stabilizePitch) {
            newLocal.x = dampAngularSpeed(localOmega.x, spinRetention);
        }
        if (stabilizeYaw) {
            newLocal.y = dampAngularSpeed(localOmega.y, spinRetention);
        }
        if (stabilizeRoll) {
            newLocal.z = dampAngularSpeed(localOmega.z, spinRetention);
        }

        if (localOmega.distanceSquared(newLocal) < 1e-14) {
            return;
        }

        Vector3d newWorld = orientation.transform(newLocal, new Vector3d());
        Vector3d delta = newWorld.sub(worldOmega, new Vector3d());
        body.addLinearAndAngularVelocity(new Vector3d(), delta);

        if (stabilizePitch) {
            wheelMomentum.x *= localOmega.x != 0 ? newLocal.x / localOmega.x : spinRetention;
        }
        if (stabilizeYaw) {
            wheelMomentum.y *= localOmega.y != 0 ? newLocal.y / localOmega.y : spinRetention;
        }
        if (stabilizeRoll) {
            wheelMomentum.z *= localOmega.z != 0 ? newLocal.z / localOmega.z : spinRetention;
        }
        syncDisplayFromWheel();
        setChanged();
    }

    private static double dampAngularSpeed(double omega, double spinRetention) {
        if (Math.abs(omega) < SETTLE_ANG_VEL && spinRetention <= 0.05) {
            return 0.0;
        }
        return omega * spinRetention;
    }

    @Override
    public void sable$getAngularVelocity(Vector3d dest) {
        dest.zero();
    }

    public void applyConfiguration(
            GyroMode mode,
            boolean autoStabilize,
            int forcePercent,
            int dampingPercent,
            int acceptAngleDeg,
            boolean stabilizePitch,
            boolean stabilizeYaw,
            boolean stabilizeRoll,
            boolean bidirectionalTorque,
            Direction downFace,
            int targetPitchDeg,
            int targetYawDeg,
            int targetRollDeg
    ) {
        this.mode = mode;
        this.autoStabilize = autoStabilize;
        this.forcePercent = Math.clamp(forcePercent, 0, 100);
        this.dampingPercent = Math.clamp(dampingPercent, 0, MAX_DAMPING_PERCENT);
        this.acceptAngleDeg = Math.clamp(acceptAngleDeg, 0, 45);
        this.stabilizePitch = stabilizePitch;
        this.stabilizeYaw = stabilizeYaw;
        this.stabilizeRoll = stabilizeRoll;
        this.bidirectionalTorque = bidirectionalTorque;
        this.downFace = downFace;
        this.targetPitchDeg = Math.clamp(targetPitchDeg, -90, 90);
        this.targetYawDeg = Math.floorMod(targetYawDeg + 180, 360) - 180;
        this.targetRollDeg = Math.clamp(targetRollDeg, -90, 90);
        setChanged();
    }

    public float getDisplayAngleX(float partialTick) {
        return (float) Math.toDegrees(displayAngles.x % (Math.PI * 2));
    }

    public float getDisplayAngleY(float partialTick) {
        return (float) Math.toDegrees(displayAngles.y % (Math.PI * 2));
    }

    public float getDisplayAngleZ(float partialTick) {
        return (float) Math.toDegrees(displayAngles.z % (Math.PI * 2));
    }

    public boolean isLit(Direction side) {
        return switch (side) {
            case NORTH -> litNorth;
            case EAST -> litEast;
            case SOUTH -> litSouth;
            case WEST -> litWest;
            default -> false;
        };
    }

    public GyroMode getMode() { return mode; }
    public boolean isAutoStabilize() { return autoStabilize; }
    public int getForcePercent() { return forcePercent; }
    public int getDampingPercent() { return dampingPercent; }
    public int getAcceptAngleDeg() { return acceptAngleDeg; }
    public boolean isStabilizePitch() { return stabilizePitch; }
    public boolean isStabilizeYaw() { return stabilizeYaw; }
    public boolean isStabilizeRoll() { return stabilizeRoll; }
    public boolean isBidirectionalTorque() { return bidirectionalTorque; }
    public Direction getDownFace() { return downFace; }
    public int getTargetPitchDeg() { return targetPitchDeg; }
    public int getTargetYawDeg() { return targetYawDeg; }
    public int getTargetRollDeg() { return targetRollDeg; }
    public boolean isNavTargetOverride() { return navTargetOverride; }

    @Override
    protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("Mode")) {
            mode = GyroMode.valueOf(tag.getString("Mode"));
        }
        autoStabilize = tag.getBoolean("AutoStabilize");
        forcePercent = tag.getInt("ForcePercent");
        if (tag.contains("DampingPercent")) {
            dampingPercent = Math.clamp(tag.getInt("DampingPercent"), 0, MAX_DAMPING_PERCENT);
        }
        if (tag.contains("AcceptAngleDeg")) {
            acceptAngleDeg = tag.getInt("AcceptAngleDeg");
        }
        stabilizePitch = tag.getBoolean("StabilizePitch");
        stabilizeYaw = tag.getBoolean("StabilizeYaw");
        stabilizeRoll = tag.getBoolean("StabilizeRoll");
        if (tag.contains("BidirectionalTorque")) {
            bidirectionalTorque = tag.getBoolean("BidirectionalTorque");
        }
        if (tag.contains("DownFace")) {
            downFace = Direction.from3DDataValue(tag.getInt("DownFace"));
        }
        if (tag.contains("TargetPitchDeg")) {
            targetPitchDeg = tag.getInt("TargetPitchDeg");
        }
        if (tag.contains("TargetYawDeg")) {
            targetYawDeg = tag.getInt("TargetYawDeg");
        }
        if (tag.contains("TargetRollDeg")) {
            targetRollDeg = tag.getInt("TargetRollDeg");
        }
        if (tag.contains("NavTargetOverride")) {
            navTargetOverride = tag.getBoolean("NavTargetOverride");
        }
        if (tag.contains("DispX")) {
            displayAngles.set(tag.getDouble("DispX"), tag.getDouble("DispY"), tag.getDouble("DispZ"));
        }
        if (tag.contains("TargetX")) {
            targetAngularVelocity.set(tag.getDouble("TargetX"), tag.getDouble("TargetY"), tag.getDouble("TargetZ"));
        }
        if (tag.contains("WheelX")) {
            wheelMomentum.set(tag.getDouble("WheelX"), tag.getDouble("WheelY"), tag.getDouble("WheelZ"));
            syncDisplayFromWheel();
        }
    }

    @Override
    public void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putString("Mode", mode.name());
        tag.putBoolean("AutoStabilize", autoStabilize);
        tag.putInt("ForcePercent", forcePercent);
        tag.putInt("DampingPercent", dampingPercent);
        tag.putInt("AcceptAngleDeg", acceptAngleDeg);
        tag.putBoolean("StabilizePitch", stabilizePitch);
        tag.putBoolean("StabilizeYaw", stabilizeYaw);
        tag.putBoolean("StabilizeRoll", stabilizeRoll);
        tag.putBoolean("BidirectionalTorque", bidirectionalTorque);
        tag.putInt("DownFace", downFace.get3DDataValue());
        tag.putInt("TargetPitchDeg", targetPitchDeg);
        tag.putInt("TargetYawDeg", targetYawDeg);
        tag.putInt("TargetRollDeg", targetRollDeg);
        tag.putBoolean("NavTargetOverride", navTargetOverride);
        tag.putDouble("TargetX", targetAngularVelocity.x);
        tag.putDouble("TargetY", targetAngularVelocity.y);
        tag.putDouble("TargetZ", targetAngularVelocity.z);
        if (!clientPacket) {
            tag.putDouble("WheelX", wheelMomentum.x);
            tag.putDouble("WheelY", wheelMomentum.y);
            tag.putDouble("WheelZ", wheelMomentum.z);
            tag.putDouble("DispX", displayAngles.x);
            tag.putDouble("DispY", displayAngles.y);
            tag.putDouble("DispZ", displayAngles.z);
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return writeClient(new CompoundTag(), registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
