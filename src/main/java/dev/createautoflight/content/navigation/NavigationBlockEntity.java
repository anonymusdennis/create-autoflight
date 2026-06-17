package dev.createautoflight.content.navigation;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockSubLevelDynamicCollider;
import dev.ryanhcode.sable.api.physics.collider.VoxelColliderData;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.createautoflight.client.ClientNavigationDebugCache;
import dev.createautoflight.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;

public class NavigationBlockEntity extends SmartBlockEntity
        implements BlockEntitySubLevelActor, BlockSubLevelDynamicCollider, AutoflightAssemblyBlock {

    private final NavigationSettings settings = new NavigationSettings();
    private final AssemblyFlightController controller = new AssemblyFlightController();
    private NavigationDebugSnapshot debugSnapshot = NavigationDebugSnapshot.empty();
    private NavStatus status = NavStatus.IDLE;
    private boolean wasActivated;
    private boolean wasRedstonePowered;
    private int debugSyncCooldown;
    private NavigationDebugSnapshot lastSyncedDebug = NavigationDebugSnapshot.empty();
    private boolean cleanedUp;

    private static final int DEBUG_SYNC_INTERVAL_TICKS = 5;
    private static final double DEBUG_SYNC_RANGE_SQ = 128.0 * 128.0;

    public NavigationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NAVIGATION.get(), pos, state);
    }

    @Override
    public void addBehaviours(List<com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            ClientNavigationDebugCache.setEnabled(worldPosition, settings.isDebugOverlayEnabled());
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
        if (!level.hasChunkAt(worldPosition)) {
            return;
        }

        SubLevelAccess containing = Sable.HELPER.getContaining(level, worldPosition);
        if (!(containing instanceof ServerSubLevel subLevel)) {
            if (!settings.isActivated() && status == NavStatus.IDLE) {
                return;
            }
            if (!settings.isActivated()) {
                clearCommand();
            }
            return;
        }

        ServerSubLevel root = AssemblyResolver.resolveRootAssembly(subLevel);
        if (!isPrimaryController(root)) {
            return;
        }

        boolean hasNavTable = NavigationTargetResolver.hasNavTable(level, worldPosition);
        boolean leverEnabled = NavigationLeverInput.isNavigationEnabled(level, worldPosition);
        boolean runtimeActivated = settings.isActivated();

        if (hasNavTable) {
            runtimeActivated = leverEnabled;
            if (leverEnabled) {
                if (!controller.isRedstoneHoldActive()) {
                    controller.onRedstoneHoldEngaged((ServerLevel) level, worldPosition, root);
                }
                wasRedstonePowered = true;
            } else {
                if (wasRedstonePowered || controller.isRedstoneHoldActive()) {
                    controller.onRedstoneHoldReleased();
                    stopNavigation(root);
                }
                wasActivated = false;
                wasRedstonePowered = false;
                return;
            }
        } else {
            wasRedstonePowered = false;
        }

        if (runtimeActivated && !wasActivated) {
            controller.onActivated(root, hasNavTable);
        } else if (!runtimeActivated && wasActivated) {
            if (!controller.isRedstoneHoldActive()) {
                controller.onDeactivated();
            }
            stopNavigation(root);
        }
        wasActivated = runtimeActivated;

        if (!runtimeActivated) {
            return;
        }

        FlightCommand command = controller.tick((ServerLevel) level, root, worldPosition, settings, runtimeActivated);
        status = controller.status();
        NavigationDebugSnapshot debug = controller.buildDebugSnapshot(root, settings, command);
        debugSnapshot = debug;

        FlightCommandBus.publish(root.getUniqueId(), worldPosition, command, debug);
        if (settings.isDebugOverlayEnabled() && ++debugSyncCooldown >= DEBUG_SYNC_INTERVAL_TICKS) {
            debugSyncCooldown = 0;
            if (!debugSnapshot.equals(lastSyncedDebug)) {
                lastSyncedDebug = debugSnapshot;
                syncDebugToClients();
            }
        } else if (!settings.isDebugOverlayEnabled()) {
            lastSyncedDebug = NavigationDebugSnapshot.empty();
        }
    }

    private void syncDebugToClients() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
        double syncX = worldPosition.getX() + 0.5;
        double syncY = worldPosition.getY() + 0.5;
        double syncZ = worldPosition.getZ() + 0.5;
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(syncX, syncY, syncZ) <= DEBUG_SYNC_RANGE_SQ) {
                player.connection.send(packet);
            }
        }
    }

    private void stopNavigation(ServerSubLevel root) {
        UUID assemblyId = root.getUniqueId();
        FlightCommandBus.setPassiveBrakingAllowed(assemblyId, settings.isIdleBraking());
        if (settings.isIdleBraking()) {
            FlightCommandBus.publish(assemblyId, worldPosition, FlightCommand.idleBraking(), NavigationDebugSnapshot.empty());
        } else {
            clearCommand(assemblyId);
        }
        status = NavStatus.IDLE;
        debugSnapshot = NavigationDebugSnapshot.empty();
        setChanged();
        if (settings.isDebugOverlayEnabled()) {
            syncDebugToClients();
        }
    }

    private boolean isPrimaryController(ServerSubLevel root) {
        BlockPos primary = findPrimaryNavigationPos(root);
        return primary != null && primary.equals(worldPosition);
    }

    private BlockPos findPrimaryNavigationPos(ServerSubLevel root) {
        BlockPos lowest = null;
        for (BlockEntitySubLevelActor actor : root.getPlot().getBlockEntityActors()) {
            if (!(actor instanceof NavigationBlockEntity nav)) {
                continue;
            }
            BlockPos pos = nav.worldPosition;
            if (lowest == null
                    || pos.getY() < lowest.getY()
                    || (pos.getY() == lowest.getY() && pos.asLong() < lowest.asLong())) {
                lowest = pos;
            }
        }
        return lowest;
    }

    private void clearCommand() {
        SubLevelAccess containing = Sable.HELPER.getContaining(level, worldPosition);
        if (containing instanceof ServerSubLevel subLevel) {
            clearCommand(AssemblyResolver.resolveRootAssembly(subLevel).getUniqueId());
        }
    }

    private void clearCommand(UUID assemblyId) {
        boolean wasActive = status != NavStatus.IDLE
                || FlightCommandBus.get(assemblyId).navActive();
        FlightCommandBus.clear(assemblyId);
        status = NavStatus.IDLE;
        debugSnapshot = NavigationDebugSnapshot.empty();
        controller.onDeactivated();
        if (wasActive) {
            setChanged();
        }
    }

    public void onRemovedFromLevel(boolean clientSide) {
        cleanupLifecycle(clientSide);
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && level.isClientSide) {
            ClientNavigationDebugCache.setEnabled(worldPosition, false);
        }
        super.onChunkUnloaded();
    }

    private void cleanupLifecycle(boolean clientSide) {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        if (!clientSide) {
            SubLevelAccess containing = level != null ? Sable.HELPER.getContaining(level, worldPosition) : null;
            if (containing instanceof ServerSubLevel subLevel) {
                FlightCommandBus.clear(AssemblyResolver.resolveRootAssembly(subLevel).getUniqueId());
            }
            controller.onDeactivated();
        } else {
            ClientNavigationDebugCache.setEnabled(worldPosition, false);
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle body, double dt) {
    }

    @Override
    public void buildBoxes(VoxelColliderData data) {
        ServerSubLevel root = subLevelFromHere();
        if (root != null) {
            ColliderShrinkHelper.buildBoxes(data, root, worldPosition, true);
        }
    }

    private ServerSubLevel subLevelFromHere() {
        if (level == null) {
            return null;
        }
        SubLevelAccess containing = Sable.HELPER.getContaining(level, worldPosition);
        if (containing instanceof ServerSubLevel subLevel) {
            return AssemblyResolver.resolveRootAssembly(subLevel);
        }
        return null;
    }

    public void applyConfiguration(
            boolean activated,
            boolean debugOverlay,
            int avoidanceOffDistance,
            int arrivalRadius,
            int cruiseSpeedPercent,
            int slowSpeedPercent,
            boolean ignoreTerrain,
            boolean idleBraking,
            boolean helicopterMode,
            boolean invertAngle,
            boolean invertThrust,
            int helicopterMaxPitchDeg,
            int navMaxThrust
    ) {
        settings.apply(activated, debugOverlay, avoidanceOffDistance, arrivalRadius,
                cruiseSpeedPercent, slowSpeedPercent, ignoreTerrain, idleBraking,
                helicopterMode, invertAngle, invertThrust, helicopterMaxPitchDeg, navMaxThrust);
        setChanged();
    }

    public NavigationSettings getSettings() { return settings; }
    public NavStatus getStatus() { return status; }
    public NavigationDebugSnapshot getDebugSnapshot() { return debugSnapshot; }

    @Override
    protected void read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (!clientPacket) {
            readSettingsFromTag(tag);
            if (tag.contains("Status")) {
                status = NavStatus.valueOf(tag.getString("Status"));
            }
            controller.readPersistence(tag);
            wasActivated = settings.isActivated();
        }
        if (clientPacket) {
            readSettingsFromTag(tag);
            if (tag.contains("Mode")) {
                debugSnapshot = NavigationDebugSnapshot.read(tag);
            }
            if (tag.contains("Status")) {
                status = NavStatus.valueOf(tag.getString("Status"));
            }
            if (level != null && level.isClientSide) {
                ClientNavigationDebugCache.setEnabled(worldPosition, settings.isDebugOverlayEnabled());
            }
        }
    }

    private void readSettingsFromTag(CompoundTag tag) {
        if (tag.contains("Activated")) {
            settings.setActivated(tag.getBoolean("Activated"));
        }
        if (tag.contains("DebugOverlay")) {
            settings.setDebugOverlayEnabled(tag.getBoolean("DebugOverlay"));
        }
        if (tag.contains("AvoidanceOff")) {
            settings.setAvoidanceOffDistance(tag.getInt("AvoidanceOff"));
        }
        if (tag.contains("ArrivalRadius")) {
            settings.setArrivalRadius(tag.getInt("ArrivalRadius"));
        }
        if (tag.contains("CruiseSpeed")) {
            settings.setCruiseSpeedPercent(tag.getInt("CruiseSpeed"));
        }
        if (tag.contains("SlowSpeed")) {
            settings.setSlowSpeedPercent(tag.getInt("SlowSpeed"));
        }
        if (tag.contains("IgnoreTerrain")) {
            settings.setIgnoreTerrain(tag.getBoolean("IgnoreTerrain"));
        }
        if (tag.contains("IdleBraking")) {
            settings.setIdleBraking(tag.getBoolean("IdleBraking"));
        }
        if (tag.contains("HelicopterMode")) {
            settings.setHelicopterMode(tag.getBoolean("HelicopterMode"));
        }
        if (tag.contains("InvertAngle")) {
            settings.setInvertAngle(tag.getBoolean("InvertAngle"));
        } else if (tag.contains("InvertDirection")) {
            settings.setInvertAngle(tag.getBoolean("InvertDirection"));
        }
        if (tag.contains("InvertThrust")) {
            settings.setInvertThrust(tag.getBoolean("InvertThrust"));
        }
        if (tag.contains("HelicopterPitch")) {
            settings.setHelicopterMaxPitchDeg(tag.getInt("HelicopterPitch"));
        }
        if (tag.contains("NavMaxThrust")) {
            settings.setNavMaxThrust(tag.getInt("NavMaxThrust"));
        }
    }

    @Override
    public void write(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!clientPacket) {
            writeSettingsToTag(tag);
            tag.putString("Status", status.name());
            controller.writePersistence(tag);
        } else {
            writeSettingsToTag(tag);
            tag.putString("Status", status.name());
            if (settings.isDebugOverlayEnabled()) {
                debugSnapshot.write(tag);
            }
        }
    }

    private void writeSettingsToTag(CompoundTag tag) {
        tag.putBoolean("Activated", settings.isActivated());
        tag.putBoolean("DebugOverlay", settings.isDebugOverlayEnabled());
        tag.putInt("AvoidanceOff", settings.getAvoidanceOffDistance());
        tag.putInt("ArrivalRadius", settings.getArrivalRadius());
        tag.putInt("CruiseSpeed", settings.getCruiseSpeedPercent());
        tag.putInt("SlowSpeed", settings.getSlowSpeedPercent());
        tag.putBoolean("IgnoreTerrain", settings.isIgnoreTerrain());
        tag.putBoolean("IdleBraking", settings.isIdleBraking());
        tag.putBoolean("HelicopterMode", settings.isHelicopterMode());
        tag.putBoolean("InvertAngle", settings.isInvertAngle());
        tag.putBoolean("InvertThrust", settings.isInvertThrust());
        tag.putInt("HelicopterPitch", settings.getHelicopterMaxPitchDeg());
        tag.putInt("NavMaxThrust", settings.getNavMaxThrust());
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
