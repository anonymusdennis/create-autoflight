package dev.createautoflight.content.thrust;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.createautoflight.content.navigation.AssemblyResolver;
import dev.createautoflight.content.navigation.AutoflightAssemblyBlock;
import dev.createautoflight.content.navigation.FlightCommand;
import dev.createautoflight.content.navigation.FlightCommandBus;
import dev.createautoflight.content.navigation.AssemblyBoundsTracker;
import dev.createautoflight.integration.aeronautics.AeronauticsThrustAccess;
import dev.createautoflight.integration.aeronautics.AssemblyThrustSample;
import dev.createautoflight.client.ClientThrustDebugCache;
import dev.createautoflight.registry.ModBlockEntities;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamicThrustControllerBlockEntity extends SmartBlockEntity
        implements BlockEntitySubLevelActor, AutoflightAssemblyBlock {

    public static final int MAX_GAIN_PERCENT = 200;
    public static final int MAX_SLEW_RPM = 128;
    public static final int MIN_TARGET_HEIGHT = -64;
    public static final int MAX_TARGET_HEIGHT = 320;
    private static final double HOVER_KP = 0.35;
    private static final double HOVER_KI = 0.08;
    private static final double HOVER_KD = 0.12;
    private static final double ALT_KP = 35.0;
    private static final double ALT_KI = 3.0;
    private static final double ALT_KD = 12.0;
    private static final double ALT_VEL_DAMP = 18.0;
    private static final double NAV_THRUST_PER_VEL = 12.0;
    private static final double CONTROL_DT = 2.0 / 20.0;

    private ThrustControlMode mode = ThrustControlMode.HOVER;
    private int gainPercent = 100;
    private int maxSlewRpm = 32;
    private boolean debugOverlay;
    private boolean holdAltitude;
    private int targetHeightY = 64;

    private final Map<Long, Float> lastThrustByGearbox = new HashMap<>();
    private final Map<Long, ThrustPidController> pidByGearbox = new HashMap<>();
    private final ThrustPidController altitudePid = new ThrustPidController();

    private float statusMeasuredY;
    private float statusHoverY;
    private float statusTargetY;
    private float statusDemandX;
    private float statusDemandY;
    private float statusDemandZ;
    private int statusGearboxCount;
    private String statusText = "Idle";
    private int debugSyncCooldown;
    private int controlTickCooldown;

    private static final int DEBUG_SYNC_INTERVAL_TICKS = 5;
    private static final int CONTROL_INTERVAL_TICKS = 2;
    private static final double DEBUG_SYNC_RANGE_SQ = 128.0 * 128.0;

    public DynamicThrustControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DYNAMIC_THRUST_CONTROLLER.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            ClientThrustDebugCache.setEnabled(worldPosition, debugOverlay);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        tickServer();
    }

    private void tickServer() {
        if (++controlTickCooldown < CONTROL_INTERVAL_TICKS) {
            return;
        }
        controlTickCooldown = 0;

        SubLevelAccess containing = Sable.HELPER.getContaining(level, worldPosition);
        if (!(containing instanceof ServerSubLevel subLevel)) {
            resetStatus("No assembly");
            clearGearboxTargets();
            return;
        }

        ServerSubLevel root = AssemblyResolver.resolveRootAssembly(subLevel);
        if (!isPrimaryController(root)) {
            return;
        }

        if (!AeronauticsThrustAccess.isAvailable()) {
            resetStatus("Aeronautics missing");
            clearGearboxTargets(root);
            return;
        }

        List<ThrustVectoringGearboxBlockEntity> gearboxes = ThrustAssemblyHelper.gearboxesOn(root);
        statusGearboxCount = gearboxes.size();
        if (gearboxes.isEmpty()) {
            resetStatus("No gearboxes");
            clearGearboxTargets();
            return;
        }

        AssemblyThrustSample sample = AeronauticsThrustAccess.measure(root);
        Vector3d center = AssemblyBoundsTracker.assemblyCenterWorld(root);
        Vector3d velocity = new Vector3d(root.latestLinearVelocity);
        statusMeasuredY = (float) center.y;
        statusHoverY = (float) sample.hoverBaselineWorld().y;
        statusTargetY = holdAltitude ? targetHeightY : statusHoverY;

        Vector3d demandSum = new Vector3d();
        FlightCommand command = FlightCommandBus.get(root.getUniqueId());
        double gain = gainPercent / 100.0;
        double controlDt = CONTROL_DT;
        double altitudeThrust = 0.0;
        if (mode == ThrustControlMode.HOVER && holdAltitude) {
            double altError = targetHeightY - center.y;
            altitudeThrust = altitudePid.update(altError, controlDt, ALT_KP, ALT_KI, ALT_KD) * gain;
            altitudeThrust -= velocity.y * ALT_VEL_DAMP * gain;
        } else {
            altitudePid.reset();
        }

        java.util.Set<Long> gearboxKeys = new java.util.HashSet<>();
        for (ThrustVectoringGearboxBlockEntity gearbox : gearboxes) {
            gearboxKeys.add(gearbox.getBlockPos().asLong());
        }
        pidByGearbox.keySet().retainAll(gearboxKeys);
        lastThrustByGearbox.keySet().retainAll(gearboxKeys);

        for (ThrustVectoringGearboxBlockEntity gearbox : gearboxes) {
            Vector3d worldAxis = ThrustAssemblyHelper.worldThrustAxis(root, gearbox.getThrustAxis());
            double measured = sample.thrustAlong(worldAxis);
            double axisDemand = computeAxisDemand(
                    mode, command, sample, worldAxis, measured, gearbox, gain, altitudeThrust, controlDt);
            demandSum.add(new Vector3d(worldAxis).mul(axisDemand));

            float thrust = slewThrustDemand(gearbox.getBlockPos(), (float) axisDemand);
            gearbox.setThrustDemand(thrust);
        }

        statusDemandX = (float) demandSum.x;
        statusDemandY = (float) demandSum.y;
        statusDemandZ = (float) demandSum.z;
        statusText = mode == ThrustControlMode.HOVER
                ? (holdAltitude ? "Hold Y=" + targetHeightY : "Hover")
                : (command.navActive() ? "Nav active" : "Nav idle");
        if (debugOverlay && ++debugSyncCooldown >= DEBUG_SYNC_INTERVAL_TICKS) {
            debugSyncCooldown = 0;
            syncToClient();
        }
    }

    private double computeAxisDemand(
            ThrustControlMode mode,
            FlightCommand command,
            AssemblyThrustSample sample,
            Vector3d worldAxis,
            double measured,
            ThrustVectoringGearboxBlockEntity gearbox,
            double gain,
            double altitudeThrust,
            double controlDt
    ) {
        ThrustPidController pid = pidByGearbox.computeIfAbsent(
                gearbox.getBlockPos().asLong(), k -> new ThrustPidController());

        if (mode == ThrustControlMode.HOVER) {
            double target = sample.hoverAlong(worldAxis);
            if (holdAltitude) {
                target += altitudeThrust * worldAxis.y;
            }
            double error = target - measured;
            return pid.update(error, controlDt, HOVER_KP, HOVER_KI, HOVER_KD) * gain;
        }

        if (!command.navActive()) {
            double target = sample.hoverAlong(worldAxis);
            double error = target - measured;
            return pid.update(error, controlDt, HOVER_KP * 0.5, HOVER_KI * 0.5, HOVER_KD) * gain;
        }

        double velAlong = command.desiredWorldVelocity().dot(worldAxis);
        double target = sample.hoverAlong(worldAxis) + velAlong * NAV_THRUST_PER_VEL * gain;
        double error = target - measured;
        double demand = pid.update(error, controlDt, HOVER_KP, HOVER_KI, HOVER_KD) * gain;
        double maxThrust = Math.max(1, command.navMaxThrust());
        return Math.clamp(demand, -maxThrust, maxThrust);
    }

    private float slewThrustDemand(BlockPos gearboxPos, float targetNewtons) {
        long key = gearboxPos.asLong();
        float previous = lastThrustByGearbox.getOrDefault(key, 0f);
        float maxStep = Math.clamp(maxSlewRpm, 1, MAX_SLEW_RPM) * ThrustVectoringGearboxBlockEntity.RPM_PER_NEWTON;
        float next = previous + Math.clamp(targetNewtons - previous, -maxStep, maxStep);
        lastThrustByGearbox.put(key, next);
        return Math.clamp(next, -ThrustVectoringGearboxBlockEntity.MAX_THRUST_NEWTONS,
                ThrustVectoringGearboxBlockEntity.MAX_THRUST_NEWTONS);
    }

    private boolean isPrimaryController(ServerSubLevel root) {
        DynamicThrustControllerBlockEntity primary = ThrustAssemblyHelper.primaryController(root);
        return primary != null && primary.worldPosition.equals(worldPosition);
    }

    private void clearGearboxTargets() {
        lastThrustByGearbox.clear();
        pidByGearbox.clear();
        altitudePid.reset();
    }

    public void onRemovedFromLevel(boolean clientSide) {
        if (clientSide) {
            ClientThrustDebugCache.setEnabled(worldPosition, false);
        } else {
            clearGearboxTargets();
        }
    }

    private void clearGearboxTargets(ServerSubLevel root) {
        for (ThrustVectoringGearboxBlockEntity gearbox : ThrustAssemblyHelper.gearboxesOn(root)) {
            gearbox.setThrustDemand(0f);
        }
        lastThrustByGearbox.clear();
        pidByGearbox.clear();
    }

    private void resetStatus(String text) {
        statusText = text;
        statusGearboxCount = 0;
        statusMeasuredY = 0;
        statusHoverY = 0;
        statusTargetY = 0;
        statusDemandX = 0;
        statusDemandY = 0;
        statusDemandZ = 0;
    }

    private void syncToClient() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
        double syncX = worldPosition.getX() + 0.5;
        double syncY = worldPosition.getY() + 0.5;
        double syncZ = worldPosition.getZ() + 0.5;
        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(syncX, syncY, syncZ) <= DEBUG_SYNC_RANGE_SQ) {
                player.connection.send(packet);
            }
        }
    }

    public void applyConfiguration(
            ThrustControlMode mode,
            int gainPercent,
            int maxSlewRpm,
            boolean debugOverlay,
            boolean holdAltitude,
            int targetHeightY
    ) {
        this.mode = mode;
        this.gainPercent = Math.clamp(gainPercent, 1, MAX_GAIN_PERCENT);
        this.maxSlewRpm = Math.clamp(maxSlewRpm, 1, MAX_SLEW_RPM);
        this.debugOverlay = debugOverlay;
        this.holdAltitude = holdAltitude;
        this.targetHeightY = Math.clamp(targetHeightY, MIN_TARGET_HEIGHT, MAX_TARGET_HEIGHT);
        pidByGearbox.clear();
        altitudePid.reset();
        setChanged();
    }

    public ThrustControlMode getMode() { return mode; }
    public int getGainPercent() { return gainPercent; }
    public int getMaxSlewRpm() { return maxSlewRpm; }
    public boolean isDebugOverlay() { return debugOverlay; }
    public boolean isHoldAltitude() { return holdAltitude; }
    public int getTargetHeightY() { return targetHeightY; }
    public float getStatusMeasuredY() { return statusMeasuredY; }
    public float getStatusHoverY() { return statusHoverY; }
    public float getStatusTargetY() { return statusTargetY; }
    public float getStatusDemandX() { return statusDemandX; }
    public float getStatusDemandY() { return statusDemandY; }
    public float getStatusDemandZ() { return statusDemandZ; }
    public int getStatusGearboxCount() { return statusGearboxCount; }
    public String getStatusText() { return statusText; }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
    }

    @Override
    protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!clientPacket) {
            readConfigFromTag(tag);
        }
        if (clientPacket) {
            readConfigFromTag(tag);
            statusMeasuredY = tag.contains("MeasY") ? tag.getFloat("MeasY") : statusMeasuredY;
            statusHoverY = tag.contains("HoverY") ? tag.getFloat("HoverY") : statusHoverY;
            statusTargetY = tag.contains("StatusTargetY") ? tag.getFloat("StatusTargetY") : statusTargetY;
            statusDemandX = tag.contains("DemX") ? tag.getFloat("DemX") : statusDemandX;
            statusDemandY = tag.contains("DemY") ? tag.getFloat("DemY") : statusDemandY;
            statusDemandZ = tag.contains("DemZ") ? tag.getFloat("DemZ") : statusDemandZ;
            statusGearboxCount = tag.contains("GbCount") ? tag.getInt("GbCount") : statusGearboxCount;
            if (tag.contains("Status")) {
                statusText = tag.getString("Status");
            }
            if (level != null && level.isClientSide) {
                ClientThrustDebugCache.setEnabled(worldPosition, debugOverlay);
            }
        }
    }

    private void readConfigFromTag(CompoundTag tag) {
        if (tag.contains("Mode")) {
            try {
                mode = ThrustControlMode.valueOf(tag.getString("Mode"));
            } catch (IllegalArgumentException ignored) {
                mode = ThrustControlMode.HOVER;
            }
        }
        if (tag.contains("Gain")) {
            gainPercent = tag.getInt("Gain");
        }
        if (tag.contains("Slew")) {
            maxSlewRpm = tag.getInt("Slew");
        }
        if (tag.contains("Debug")) {
            debugOverlay = tag.getBoolean("Debug");
        }
        if (tag.contains("HoldAlt")) {
            holdAltitude = tag.getBoolean("HoldAlt");
        }
        if (tag.contains("TargetY")) {
            targetHeightY = Math.clamp(tag.getInt("TargetY"), MIN_TARGET_HEIGHT, MAX_TARGET_HEIGHT);
        }
    }

    private void writeConfigToTag(CompoundTag tag) {
        tag.putString("Mode", mode.name());
        tag.putInt("Gain", gainPercent);
        tag.putInt("Slew", maxSlewRpm);
        tag.putBoolean("Debug", debugOverlay);
        tag.putBoolean("HoldAlt", holdAltitude);
        tag.putInt("TargetY", targetHeightY);
    }

    @Override
    public void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!clientPacket) {
            writeConfigToTag(tag);
        } else {
            writeConfigToTag(tag);
            tag.putFloat("MeasY", statusMeasuredY);
            tag.putFloat("HoverY", statusHoverY);
            tag.putFloat("StatusTargetY", statusTargetY);
            tag.putFloat("DemX", statusDemandX);
            tag.putFloat("DemY", statusDemandY);
            tag.putFloat("DemZ", statusDemandZ);
            tag.putInt("GbCount", statusGearboxCount);
            tag.putString("Status", statusText);
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
