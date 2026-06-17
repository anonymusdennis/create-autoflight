package dev.createautoflight.content.thruster;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockSubLevelDynamicCollider;
import dev.ryanhcode.sable.api.physics.collider.VoxelColliderData;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.createautoflight.content.navigation.ApproachPhase;
import dev.createautoflight.content.navigation.AssemblyResolver;
import dev.createautoflight.content.navigation.AutoflightAssemblyBlock;
import dev.createautoflight.content.navigation.ColliderShrinkHelper;
import dev.createautoflight.content.navigation.FlightCommand;
import dev.createautoflight.content.navigation.FlightCommandBus;
import dev.createautoflight.content.navigation.ThrusterFleetProfiler;
import dev.createautoflight.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

/**
 * Inertial brake thruster — viscous velocity damping toward zero linear speed.
 */
public class ThrusterBlockEntity extends SmartBlockEntity
        implements BlockEntitySubLevelActor, BlockSubLevelDynamicCollider, AutoflightAssemblyBlock {

    public enum ThrusterMode {
        BRAKE,
        NAVIGATION
    }

    public static final int MAX_STRENGTH_PERCENT = 400;
    public static final int DEFAULT_STRENGTH_PERCENT = 100;
    public static final int DEFAULT_MAX_THRUST = 8;
    public static final int MAX_THRUST_LIMIT = 64;

    /** Stay on while root-assembly speed is above this. */
    private static final double BRAKE_ON_SPEED = 0.005;
    /** Release only after speed stays below this for {@link #RELEASE_HOLD_TICKS} game ticks. */
    private static final double BRAKE_OFF_SPEED = 0.002;
    /** Consecutive game ticks below release speed before disengaging. */
    private static final int RELEASE_HOLD_TICKS = 30;
    /** Speed scale for soft landing — thrust tapers as {@code v / (v + SETTLE_SPEED)}. */
    private static final double SETTLE_SPEED = 0.2;
    /** Max fraction of assembly momentum removable per physics step at 400% response. */
    private static final double MAX_MOMENTUM_FRACTION = 0.5;
    private static final double NOZZLE_OFFSET = 0.45;
    private static final double EXHAUST_SPAWN_JITTER = 0.08;
    private static final double EXHAUST_BASE_SPEED = 0.6;
    private static final double EXHAUST_INTENSITY_SPEED = 1.4;
    private static final double CLIENT_PHYSICS_DT = 1.0 / 20.0;
    private static final float INTENSITY_SMOOTHING = 0.2f;
    private static final float ENGAGED_INTENSITY_FLOOR = 0.12f;

    /** Nozzle directions used for exhaust visualization (all six axes). */
    private static final Direction[] NOZZLE_DIRECTIONS = Direction.values();

    private boolean enabled = true;
    private ThrusterMode mode = ThrusterMode.BRAKE;
    private int strengthPercent = DEFAULT_STRENGTH_PERCENT;
    private int maxThrust = DEFAULT_MAX_THRUST;
    private boolean smokeParticles = true;
    /** Flips nav thrust direction for blocks mounted facing the opposite way. */
    private boolean invertDirection;

    /** Latched while the assembly is being braked — prevents on/off flicker at low speed. */
    private boolean brakingEngaged;
    /** Latched while navigation thrust is active. */
    private boolean navEngaged;
    private final Vector3d lastNavDirection = new Vector3d(0, 0, 1);
    private int releaseDelay;
    private final Vector3d lastBrakeDirection = new Vector3d(0, 0, 1);
    /** Smoothed 0–1 value for particles; synced to client only when engagement flips. */
    private float displayIntensity;

    public ThrusterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.THRUSTER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            tickClientParticles();
        } else {
            tickServerEngagement();
        }
    }

    /** Engagement latch runs once per game tick — not per physics substep. */
    private void tickServerEngagement() {
        SubLevelAccess containing = Sable.HELPER.getContaining(level, worldPosition);
        if (containing == null) {
            setBrakingEngaged(false);
            setNavEngaged(false);
            return;
        }

        ServerSubLevel rootAssembly = containing instanceof ServerSubLevel serverSubLevel
                ? AssemblyResolver.resolveRootAssembly(serverSubLevel)
                : null;

        Vector3d worldVel = rootAssembly != null
                ? new Vector3d(rootAssembly.latestLinearVelocity)
                : Sable.HELPER.getVelocity(level, containing, JOMLConversion.atCenterOf(worldPosition), new Vector3d());

        if (mode == ThrusterMode.NAVIGATION && rootAssembly != null) {
            tickNavEngagement(rootAssembly, worldVel);
            if (navEngaged) {
                setBrakingEngaged(false);
            }
            return;
        }

        FlightCommand cmd = rootAssembly != null
                ? FlightCommandBus.get(rootAssembly.getUniqueId())
                : FlightCommand.idle();

        if (rootAssembly != null && cmd.helicopterAssist() && isHelicopterAssistCandidate()) {
            tickHelicopterAssistEngagement(rootAssembly, worldVel, cmd);
            if (navEngaged) {
                setBrakingEngaged(false);
            }
            return;
        }

        setNavEngaged(false);

        if (mode == ThrusterMode.BRAKE && cmd.requestBrakeAssist() && isBrakingCandidate()) {
            tickApproachBrakeEngagement(worldVel, cmd);
            return;
        }

        if (!cmd.navActive() && rootAssembly != null
                && !FlightCommandBus.isPassiveBrakingAllowed(rootAssembly.getUniqueId())) {
            setBrakingEngaged(false);
            return;
        }

        if (!isBrakingCandidate()) {
            setBrakingEngaged(false);
            return;
        }

        updateBrakingEngagement(worldVel.length(), worldVel);
    }

    private void tickApproachBrakeEngagement(Vector3d worldVel, FlightCommand cmd) {
        double speed = worldVel.length();
        double desiredSpeed = cmd.desiredWorldVelocity().length();
        if (speed > desiredSpeed + 0.02 || (cmd.approachPhase() == ApproachPhase.DOCKED && speed > 0.01)) {
            brakingEngaged = true;
            releaseDelay = 0;
            if (speed > 1e-6) {
                lastBrakeDirection.set(worldVel).normalize();
            }
        } else {
            setBrakingEngaged(false);
        }
    }

    private void tickNavEngagement(ServerSubLevel root, Vector3d worldVel) {
        FlightCommand cmd = FlightCommandBus.get(root.getUniqueId());
        boolean assist = cmd.helicopterAssist();
        if (!assist && !isNavCandidate()) {
            setNavEngaged(false);
            return;
        }
        if (assist && !isHelicopterAssistCandidate()) {
            setNavEngaged(false);
            return;
        }
        boolean shouldEngage = cmd.navActive() && (!cmd.blockTranslation() || assist);
        if (shouldEngage) {
            Vector3d desired = cmd.desiredWorldVelocity();
            Vector3d delta = new Vector3d(desired).sub(worldVel);
            if (delta.lengthSquared() > 1e-8) {
                lastNavDirection.set(delta).normalize();
            }
        }
        setNavEngaged(shouldEngage && (
                desiredThrustMagnitude(root, worldVel, cmd) > 1e-6
                        || cmd.desiredWorldVelocity().lengthSquared() > 2.5e-3
        ));
    }

    private void tickHelicopterAssistEngagement(ServerSubLevel root, Vector3d worldVel, FlightCommand cmd) {
        boolean shouldEngage = cmd.navActive() && (!cmd.blockTranslation() || cmd.helicopterAssist());
        if (shouldEngage) {
            Vector3d desired = cmd.desiredWorldVelocity();
            Vector3d delta = new Vector3d(desired).sub(worldVel);
            if (delta.lengthSquared() > 1e-8) {
                lastNavDirection.set(delta).normalize();
            }
        }
        setNavEngaged(shouldEngage && (
                desiredThrustMagnitude(root, worldVel, cmd) > 1e-6
                        || cmd.desiredWorldVelocity().lengthSquared() > 2.5e-3
        ));
    }

    private double desiredThrustMagnitude(ServerSubLevel root, Vector3d worldVel, FlightCommand cmd) {
        Vector3d delta = new Vector3d(cmd.desiredWorldVelocity()).sub(worldVel);
        return delta.length();
    }

    private void tickClientParticles() {
        if (!smokeParticles || !enabled || strengthPercent <= 0 || maxThrust <= 0) {
            smoothDisplayIntensity(0);
            return;
        }

        if (!brakingEngaged && !navEngaged) {
            smoothDisplayIntensity(0);
            return;
        }

        ThrustPlan plan = navEngaged
                ? computeNavThrustPlan(level, worldPosition, strengthPercent, maxThrust, CLIENT_PHYSICS_DT)
                : computeThrustPlan(level, worldPosition, strengthPercent, maxThrust, CLIENT_PHYSICS_DT);
        float targetIntensity = plan != null ? plan.intensity : ENGAGED_INTENSITY_FLOOR;
        smoothDisplayIntensity(Math.max(targetIntensity, ENGAGED_INTENSITY_FLOOR));
        if (displayIntensity <= 0.01f) {
            return;
        }

        Vector3d thrustDir = navEngaged
                ? new Vector3d(lastNavDirection)
                : new Vector3d(lastBrakeDirection).negate();
        if (thrustDir.lengthSquared() < 1e-8) {
            return;
        }
        thrustDir.normalize();
        Vector3d exhaustDir = new Vector3d(thrustDir).negate();

        RandomSource random = level.random;
        int particleBudget = Math.max(1, Math.round(displayIntensity * 4));

        // Spawn at the nozzle mouth (offset along the exhaust axis) and eject hard along it.
        // LARGE_SMOKE (a rising particle) dampens its initial velocity and adds buoyancy, so a
        // small speed reads as a gentle drift; bias the spawn and use a high ejection speed that
        // scales with thrust intensity so the exhaust looks expelled rather than eased out.
        for (int i = 0; i < particleBudget; i++) {
            double px = worldPosition.getX() + 0.5 + exhaustDir.x * NOZZLE_OFFSET + (random.nextDouble() - 0.5) * EXHAUST_SPAWN_JITTER;
            double py = worldPosition.getY() + 0.5 + exhaustDir.y * NOZZLE_OFFSET + (random.nextDouble() - 0.5) * EXHAUST_SPAWN_JITTER;
            double pz = worldPosition.getZ() + 0.5 + exhaustDir.z * NOZZLE_OFFSET + (random.nextDouble() - 0.5) * EXHAUST_SPAWN_JITTER;

            double exhaustSpeed = EXHAUST_BASE_SPEED + displayIntensity * EXHAUST_INTENSITY_SPEED;
            level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    px, py, pz,
                    exhaustDir.x * exhaustSpeed,
                    exhaustDir.y * exhaustSpeed,
                    exhaustDir.z * exhaustSpeed
            );
        }
    }

    private void smoothDisplayIntensity(float target) {
        if (brakingEngaged && target > 0 && target < ENGAGED_INTENSITY_FLOOR) {
            target = ENGAGED_INTENSITY_FLOOR;
        }
        displayIntensity += (target - displayIntensity) * INTENSITY_SMOOTHING;
        if (target <= 0 && displayIntensity < 0.01f) {
            displayIntensity = 0;
        }
    }

    private static int pickNozzleIndex(double[] weights, RandomSource random) {
        double sum = 0.0;
        for (double weight : weights) {
            sum += weight;
        }
        if (sum < 1e-8) {
            return -1;
        }

        double pick = random.nextDouble() * sum;
        double running = 0.0;
        for (int i = 0; i < weights.length; i++) {
            running += weights[i];
            if (pick <= running) {
                return i;
            }
        }
        return weights.length - 1;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
        if (level == null || level.isClientSide || !enabled || strengthPercent <= 0 || maxThrust <= 0) {
            return;
        }

        if (mode == ThrusterMode.NAVIGATION && navEngaged) {
            applyNavThrust(subLevel, dt);
            return;
        }

        if (navEngaged) {
            applyNavThrust(subLevel, dt);
            return;
        }

        if (!brakingEngaged) {
            return;
        }

        ThrustPlan plan = computeThrustPlan(level, worldPosition, strengthPercent, maxThrust, dt);
        if (plan == null) {
            return;
        }

        QueuedForceGroup propulsion = subLevel.getOrCreateQueuedForceGroup(
                ForceGroups.REGISTRY.get(ResourceLocation.fromNamespaceAndPath("sable", "propulsion"))
        );
        Vector3d blockCenter = JOMLConversion.atCenterOf(worldPosition);
        propulsion.applyAndRecordPointForce(blockCenter, plan.counterImpulse);
    }

    private void applyNavThrust(ServerSubLevel subLevel, double dt) {
        ServerSubLevel root = AssemblyResolver.resolveRootAssembly(subLevel);
        FlightCommand cmd = FlightCommandBus.get(root.getUniqueId());
        if (!cmd.navActive() || (cmd.blockTranslation() && !cmd.helicopterAssist())) {
            return;
        }

        Vector3d worldVel = new Vector3d(root.latestLinearVelocity);
        Vector3d delta = new Vector3d(cmd.desiredWorldVelocity()).sub(worldVel);
        if (delta.lengthSquared() < 1e-10) {
            return;
        }

        int activeNavThrusters = cmd.helicopterAssist()
                ? countHelicopterAssistThrusters(root)
                : countActiveNavThrusters(root);
        ThrusterFleetProfiler.FleetProfile fleet = ThrusterFleetProfiler.navProfile(root);
        if (cmd.navMaxThrust() > 0) {
            fleet = new ThrusterFleetProfiler.FleetProfile(
                    cmd.navMaxThrust(),
                    activeNavThrusters,
                    fleet.maxAcceleration()
            );
        }
        double strength = strengthPercent / 100.0;
        int fleetThrust = cmd.navMaxThrust();
        double thrustCap = fleetThrust * strength * dt / Math.max(1, activeNavThrusters);
        double deltaLen = delta.length();
        if (deltaLen < 1e-10) {
            return;
        }
        Vector3d impulse = new Vector3d(delta).mul(Math.min(thrustCap, deltaLen) / deltaLen);
        if (invertDirection) {
            impulse.negate();
        }
        impulse = ThrusterFleetProfiler.scaleByForce(impulse, fleet, dt);

        QueuedForceGroup propulsion = subLevel.getOrCreateQueuedForceGroup(
                ForceGroups.REGISTRY.get(ResourceLocation.fromNamespaceAndPath("sable", "propulsion"))
        );
        propulsion.applyAndRecordPointForce(JOMLConversion.atCenterOf(worldPosition), impulse);
    }

    @Override
    public void buildBoxes(VoxelColliderData data) {
        SubLevelAccess containing = level != null ? Sable.HELPER.getContaining(level, worldPosition) : null;
        if (containing instanceof ServerSubLevel subLevel) {
            ColliderShrinkHelper.buildBoxes(data, AssemblyResolver.resolveRootAssembly(subLevel), worldPosition, false);
        }
    }

    private ThrustPlan computeNavThrustPlan(
            Level level,
            BlockPos pos,
            int strengthPercent,
            int maxThrust,
            double dt
    ) {
        if (!navEngaged) {
            return null;
        }
        SubLevelAccess containing = Sable.HELPER.getContaining(level, pos);
        if (containing == null) {
            return null;
        }
        Quaterniond orientation = new Quaterniond(containing.logicalPose().orientation());
        double strength = strengthPercent / 100.0;
        double impulseMag = maxThrust * strength * dt;
        Vector3d thrustDir = new Vector3d(lastNavDirection);
        if (invertDirection) {
            thrustDir.negate();
        }
        Vector3d impulse = new Vector3d(thrustDir).mul(impulseMag);
        double[] nozzleWeights = nozzleWeightsForDirection(orientation, thrustDir);
        return new ThrustPlan(impulse, nozzleWeights, 1f);
    }

    private ThrustPlan computeThrustPlan(
            Level level,
            BlockPos pos,
            int strengthPercent,
            int maxThrust,
            double dt
    ) {
        if (!enabled || strengthPercent <= 0 || maxThrust <= 0) {
            return null;
        }

        SubLevelAccess containing = Sable.HELPER.getContaining(level, pos);
        if (containing == null) {
            return null;
        }

        ServerSubLevel rootAssembly = containing instanceof ServerSubLevel serverSubLevel
                ? AssemblyResolver.resolveRootAssembly(serverSubLevel)
                : null;

        Vector3d worldVel = rootAssembly != null
                ? new Vector3d(rootAssembly.latestLinearVelocity)
                : Sable.HELPER.getVelocity(level, containing, JOMLConversion.atCenterOf(pos), new Vector3d());

        if (!brakingEngaged) {
            return null;
        }

        double speed = worldVel.length();
        Quaterniond orientation = new Quaterniond(containing.logicalPose().orientation());
        MassData rootMass = rootAssembly != null ? rootAssembly.getMassTracker() : null;
        int activeThrusters = rootAssembly != null ? countActiveThrusters(rootAssembly) : 1;

        return computeThrustPlanFromVelocity(
                worldVel,
                orientation,
                strengthPercent,
                maxThrust,
                dt,
                rootMass,
                activeThrusters,
                speed > 1e-6 ? new Vector3d(worldVel).div(speed) : new Vector3d(lastBrakeDirection)
        );
    }

    private void updateBrakingEngagement(double speed, Vector3d worldVel) {
        boolean wasEngaged = brakingEngaged;

        if (speed > BRAKE_ON_SPEED) {
            brakingEngaged = true;
            releaseDelay = 0;
            if (speed > 1e-6) {
                lastBrakeDirection.set(worldVel).normalize();
            }
        } else if (brakingEngaged) {
            if (speed > BRAKE_OFF_SPEED) {
                releaseDelay = 0;
                if (speed > 1e-6) {
                    lastBrakeDirection.set(worldVel).normalize();
                }
            } else {
                releaseDelay++;
                if (releaseDelay >= RELEASE_HOLD_TICKS) {
                    brakingEngaged = false;
                    releaseDelay = 0;
                }
            }
        }

        if (brakingEngaged != wasEngaged) {
            if (!brakingEngaged) {
                displayIntensity = 0;
            }
            if (level != null && !level.isClientSide) {
                setChanged();
            }
        }
    }

    private static ThrustPlan computeThrustPlanFromVelocity(
            Vector3d worldVel,
            Quaterniond orientation,
            int strengthPercent,
            int maxThrust,
            double dt,
            MassData rootMass,
            int activeThrusters,
            Vector3d brakeDir
    ) {
        double speed = worldVel.length();
        double effectiveSpeed = Math.max(speed, BRAKE_OFF_SPEED * 0.5);

        double strength = strengthPercent / 100.0;
        double response = Math.min(strength / (MAX_STRENGTH_PERCENT / 100.0), 1.0);

        double settleFactor = effectiveSpeed / (effectiveSpeed + SETTLE_SPEED);
        double thrustImpulseCap = maxThrust * strength * dt;

        double impulseMag;
        if (rootMass == null || rootMass.isInvalid()) {
            impulseMag = thrustImpulseCap * settleFactor;
        } else {
            double mass = rootMass.getMass();
            int thrusters = Math.max(1, activeThrusters);

            double fleetMomentumBudget = mass * effectiveSpeed * MAX_MOMENTUM_FRACTION * response * settleFactor;
            double perThrusterMomentumCap = fleetMomentumBudget / thrusters;
            double noReverseCap = mass * effectiveSpeed / thrusters;

            impulseMag = Math.min(thrustImpulseCap, Math.min(perThrusterMomentumCap, noReverseCap));
        }

        if (impulseMag < 1e-8) {
            impulseMag = 1e-8;
        }

        Vector3d counterImpulse = new Vector3d(brakeDir).negate().mul(impulseMag);

        double[] nozzleWeights = new double[NOZZLE_DIRECTIONS.length];
        for (int i = 0; i < NOZZLE_DIRECTIONS.length; i++) {
            Vector3d worldNozzleDir = orientation.transform(axisDirection(NOZZLE_DIRECTIONS[i]), new Vector3d());
            nozzleWeights[i] = Math.max(0.0, worldNozzleDir.dot(brakeDir));
        }

        float intensity = thrustImpulseCap > 1e-8
                ? (float) Math.min(impulseMag / thrustImpulseCap, 1.0)
                : 0f;
        return new ThrustPlan(counterImpulse, nozzleWeights, intensity);
    }

    private void setBrakingEngaged(boolean engaged) {
        if (brakingEngaged == engaged) {
            return;
        }
        brakingEngaged = engaged;
        releaseDelay = 0;
        if (!engaged) {
            displayIntensity = 0;
        }
        if (level != null && !level.isClientSide) {
            setChanged();
        }
    }

    private static int countActiveThrusters(ServerSubLevel rootAssembly) {
        int count = 0;
        for (BlockEntitySubLevelActor actor : rootAssembly.getPlot().getBlockEntityActors()) {
            if (actor instanceof ThrusterBlockEntity thruster && thruster.isBrakingCandidate()) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    private static int countActiveNavThrusters(ServerSubLevel rootAssembly) {
        int count = 0;
        for (BlockEntitySubLevelActor actor : rootAssembly.getPlot().getBlockEntityActors()) {
            if (actor instanceof ThrusterBlockEntity thruster && thruster.isNavCandidate()) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    /** All enabled thrusters on the assembly may assist helicopter flight. */
    private static int countHelicopterAssistThrusters(ServerSubLevel rootAssembly) {
        int count = 0;
        for (BlockEntitySubLevelActor actor : rootAssembly.getPlot().getBlockEntityActors()) {
            if (actor instanceof ThrusterBlockEntity thruster && thruster.isHelicopterAssistCandidate()) {
                count++;
            }
        }
        return Math.max(1, count);
    }

    public boolean isHelicopterAssistCandidate() {
        return enabled && strengthPercent > 0 && maxThrust > 0;
    }

    private double helicopterAssistScale(ServerSubLevel subLevel, Vector3d impulse) {
        if (impulse.lengthSquared() < 1e-12) {
            return 1.0;
        }
        Quaterniond orientation = new Quaterniond(subLevel.logicalPose().orientation());
        Vector3d thrustDir = new Vector3d(impulse).normalize();
        double[] weights = nozzleWeightsForDirection(orientation, thrustDir);
        double best = 0.0;
        for (double weight : weights) {
            best = Math.max(best, weight);
        }
        return Math.max(0.35, best);
    }

    public boolean isNavCandidate() {
        return isFleetCandidate(ThrusterMode.NAVIGATION);
    }

    public boolean isFleetCandidate(ThrusterMode fleetMode) {
        return mode == fleetMode && enabled && strengthPercent > 0 && maxThrust > 0;
    }

    private boolean isBrakingCandidate() {
        return isFleetCandidate(ThrusterMode.BRAKE);
    }

    private static Vector3d axisDirection(Direction dir) {
        return new Vector3d(dir.getStepX(), dir.getStepY(), dir.getStepZ());
    }

    private static double[] nozzleWeightsForDirection(Quaterniond orientation, Vector3d brakeDir) {
        double[] weights = new double[NOZZLE_DIRECTIONS.length];
        for (int i = 0; i < NOZZLE_DIRECTIONS.length; i++) {
            Vector3d worldNozzleDir = orientation.transform(axisDirection(NOZZLE_DIRECTIONS[i]), new Vector3d());
            weights[i] = Math.max(0.0, worldNozzleDir.dot(brakeDir));
        }
        return weights;
    }

    private void setNavEngaged(boolean engaged) {
        if (navEngaged == engaged) {
            return;
        }
        navEngaged = engaged;
        if (level != null && !level.isClientSide) {
            setChanged();
        }
    }

    public void applyConfiguration(
            boolean enabled,
            ThrusterMode mode,
            int strengthPercent,
            int maxThrust,
            boolean smokeParticles,
            boolean invertDirection
    ) {
        this.enabled = enabled;
        this.mode = mode;
        this.strengthPercent = Math.clamp(strengthPercent, 0, MAX_STRENGTH_PERCENT);
        this.maxThrust = Math.clamp(maxThrust, 0, MAX_THRUST_LIMIT);
        this.smokeParticles = smokeParticles;
        this.invertDirection = invertDirection;
        if (!isBrakingCandidate()) {
            setBrakingEngaged(false);
        }
        if (!isNavCandidate()) {
            setNavEngaged(false);
        }
        setChanged();
    }

    public boolean isEnabled() { return enabled; }
    public ThrusterMode getMode() { return mode; }
    public int getStrengthPercent() { return strengthPercent; }
    public int getMaxThrust() { return maxThrust; }
    public boolean isSmokeParticles() { return smokeParticles; }
    public boolean isInvertDirection() { return invertDirection; }
    public boolean isBrakingEngaged() { return brakingEngaged; }
    public boolean isNavEngaged() { return navEngaged; }
    public float getDisplayIntensity() { return displayIntensity; }

    @Override
    protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
        if (tag.contains("Mode")) {
            mode = ThrusterMode.valueOf(tag.getString("Mode"));
        }
        strengthPercent = tag.contains("StrengthPercent")
                ? Math.clamp(tag.getInt("StrengthPercent"), 0, MAX_STRENGTH_PERCENT)
                : DEFAULT_STRENGTH_PERCENT;
        maxThrust = tag.contains("MaxThrust")
                ? Math.clamp(tag.getInt("MaxThrust"), 0, MAX_THRUST_LIMIT)
                : DEFAULT_MAX_THRUST;
        smokeParticles = !tag.contains("SmokeParticles") || tag.getBoolean("SmokeParticles");
        if (tag.contains("InvertDirection")) {
            invertDirection = tag.getBoolean("InvertDirection");
        }
        brakingEngaged = tag.getBoolean("BrakingEngaged");
        navEngaged = tag.getBoolean("NavEngaged");
        if (tag.contains("LastBrakeX")) {
            lastBrakeDirection.set(tag.getDouble("LastBrakeX"), tag.getDouble("LastBrakeY"), tag.getDouble("LastBrakeZ"));
        }
    }

    @Override
    public void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Enabled", enabled);
        tag.putString("Mode", mode.name());
        tag.putInt("StrengthPercent", strengthPercent);
        tag.putInt("MaxThrust", maxThrust);
        tag.putBoolean("SmokeParticles", smokeParticles);
        tag.putBoolean("InvertDirection", invertDirection);
        tag.putBoolean("BrakingEngaged", brakingEngaged);
        tag.putBoolean("NavEngaged", navEngaged);
        tag.putDouble("LastBrakeX", lastBrakeDirection.x);
        tag.putDouble("LastBrakeY", lastBrakeDirection.y);
        tag.putDouble("LastBrakeZ", lastBrakeDirection.z);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return writeClient(new CompoundTag(), registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private record ThrustPlan(
            Vector3d counterImpulse,
            double[] nozzleWeights,
            float intensity
    ) {}
}
