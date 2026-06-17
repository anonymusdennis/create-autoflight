package dev.ryanhcode.sable.sublevel;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.api.physics.mass.MergedMassTracker;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundChangeSubLevelNamePacket;
import dev.ryanhcode.sable.physics.ReactionWheelManager;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockController;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.LevelAccelerator;
import foundry.veil.api.network.VeilPacketManager.PacketSink;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;

public class ServerSubLevel extends SubLevel implements PhysicsPipelineBody {
   @Internal
   public final Vector3d latestLinearVelocity = new Vector3d();
   @Internal
   public final Vector3d latestAngularVelocity = new Vector3d();
   private final Set<UUID> trackingPlayers = new ObjectOpenHashSet();
   private final Pose3d lastNetworkedPose = new Pose3d();
   private final BoundingBox3i lastNetworkedBoundingBox = new BoundingBox3i();
   private final int runtimeId;
   private final SubLevelHeatMapManager heatMapManager = new SubLevelHeatMapManager(this);
   private final FloatingBlockController floatingBlockController = new FloatingBlockController(this);
   private final ReactionWheelManager reactionWheelManager = new ReactionWheelManager(this);
   @Nullable
   private Object2ObjectMap<ForceGroup, QueuedForceGroup> queuedForceGroups = null;
   private MergedMassTracker massTracker;
   private boolean lastNetworkedStopped = false;
   @Nullable
   private UUID splitFromSubLevel = null;
   @Nullable
   private Pose3d splitFromPose = null;
   @Internal
   private GlobalSavedSubLevelPointer lastSerializationPointer = null;
   @Nullable
   private CompoundTag userDataTag;
   private boolean trackIndividualQueuedForces = false;

   public ServerSubLevel(ServerLevel level, int plotX, int plotY, Pose3d pose) {
      super(level, plotX, plotY, pose);
      SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);

      assert physicsSystem != null;

      this.runtimeId = physicsSystem.getNextRuntimeID();
   }

   public Collection<UUID> getTrackingPlayers() {
      return this.trackingPlayers;
   }

   public PacketSink playerSink() {
      return packet -> {
         for (UUID uuid : this.trackingPlayers) {
            ServerPlayer player = (ServerPlayer)this.getLevel().getPlayerByUUID(uuid);
            if (player instanceof ServerPlayer) {
               player.connection.send(packet);
            }
         }
      };
   }

   public Pose3d lastNetworkedPose() {
      return this.lastNetworkedPose;
   }

   public BoundingBox3i lastNetworkedBoundingBox() {
      return this.lastNetworkedBoundingBox;
   }

   @Override
   public int getRuntimeId() {
      return this.runtimeId;
   }

   @Override
   protected LevelPlot createPlot(SubLevelContainer plotContainer, int plotX, int plotY, int logPlotSize) {
      return new ServerLevelPlot(plotContainer, plotX, plotY, plotContainer.getLogPlotSize(), this);
   }

   @Internal
   @Override
   public void onPlotBoundsChanged() {
      BoundingBox3ic bounds = this.getPlot().getBoundingBox();
      if (bounds == BoundingBox3i.EMPTY || bounds.volume() <= 0) {
         this.markRemoved();
      }
   }

   @Internal
   @Override
   public void tick() {
      super.tick();
      this.updateBoundingBox();
      BoundingBox3dc bounds = this.boundingBox();
      if (this.isRemoved()
         || !(bounds.minY() < SableConfig.SUB_LEVEL_REMOVE_MIN.getAsDouble()) && !(bounds.maxY() > SableConfig.SUB_LEVEL_REMOVE_MAX.getAsDouble())) {
         if (SableConfig.SUB_LEVEL_SPLITTING.getAsBoolean()) {
            this.heatMapManager.tick();
         }
      } else {
         Sable.LOGGER.info("Sub-level {} has an extreme Y coordinate range, removing", this);
         this.markRemoved();
      }
   }

   @Internal
   public boolean getLastNetworkedStopped() {
      return this.lastNetworkedStopped;
   }

   @Internal
   public void setLastNetworkedStopped(boolean stopped) {
      this.lastNetworkedStopped = stopped;
   }

   @Internal
   public void updateMergedMassData(float partialPhysicsTick) {
      if (this.massTracker != null) {
         this.massTracker.update(partialPhysicsTick);
      }
   }

   @Internal
   public void prePhysicsTickBegin() {
      if (this.queuedForceGroups != null) {
         this.queuedForceGroups.values().forEach(QueuedForceGroup::reset);
      }
   }

   public void applyQueuedForces(SubLevelPhysicsSystem physicsSystem, RigidBodyHandle handle, double timeStep) {
      if (this.queuedForceGroups != null) {
         ObjectIterator var5 = this.queuedForceGroups.entrySet().iterator();

         while (var5.hasNext()) {
            Entry<ForceGroup, QueuedForceGroup> entry = (Entry<ForceGroup, QueuedForceGroup>)var5.next();
            QueuedForceGroup group = entry.getValue();
            handle.applyForcesAndReset(group.getForceTotal());
         }
      }
   }

   @Internal
   public void prePhysicsTick(SubLevelPhysicsSystem physicsSystem, RigidBodyHandle handle, double timeStep) {
      ServerLevelPlot plot = this.getPlot();

      for (BlockEntitySubLevelActor actor : plot.getBlockEntityActors()) {
         actor.sable$physicsTick(this, handle, timeStep);
      }

      ObjectCollection<BlockSubLevelLiftProvider.LiftProviderContext> liftProviders = plot.getLiftProviders();
      Collection<KinematicContraption> contraptions = plot.getContraptions();
      if (!liftProviders.isEmpty() || this.floatingBlockController.needsTicking() || this.reactionWheelManager.needsTicking() || !contraptions.isEmpty()) {
         boolean trackForces = this.isTrackingIndividualQueuedForces();
         Vector3d linearVelocity = handle.getLinearVelocity(new Vector3d());
         Vector3d angularVelocity = handle.getAngularVelocity(new Vector3d());
         Vector3d linearImpulse = new Vector3d();
         Vector3d angularImpulse = new Vector3d();
         List<BlockSubLevelLiftProvider.LiftProviderGroup> groups = trackForces ? BlockSubLevelLiftProvider.groupLiftProviders(liftProviders) : List.of();
         ObjectIterator localContraptionPose = liftProviders.iterator();

         while (localContraptionPose.hasNext()) {
            BlockSubLevelLiftProvider.LiftProviderContext context = (BlockSubLevelLiftProvider.LiftProviderContext)localContraptionPose.next();
            BlockSubLevelLiftProvider.LiftProviderGroup group = null;

            for (BlockSubLevelLiftProvider.LiftProviderGroup g : groups) {
               if (g.positions().contains(context.pos())) {
                  group = g;
                  break;
               }
            }

            ((BlockSubLevelLiftProvider)context.state().getBlock())
               .sable$contributeLiftAndDrag(context, this, null, timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse, group);
         }

         for (BlockSubLevelLiftProvider.LiftProviderGroup group : groups) {
            if (group.totalLift().lengthSquared() >= 1.0E-6) {
               this.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.LIFT.get())
                  .recordPointForce(group.liftCenter().div(group.totalLiftStrength), group.totalLift());
            }

            if (group.totalDrag().lengthSquared() >= 1.0E-6) {
               this.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.DRAG.get())
                  .recordPointForce(group.dragCenter().div(group.totalDragStrength), group.totalDrag());
            }
         }

         if (!contraptions.isEmpty()) {
            Pose3d localContraptionPosex = new Pose3d();

            for (KinematicContraption contraption : contraptions) {
               Collection<BlockSubLevelLiftProvider.LiftProviderContext> contraptionProviders = contraption.sable$liftProviders().values();
               contraption.sable$getLocalPose(localContraptionPosex, physicsSystem.getPartialPhysicsTick());
               List<BlockSubLevelLiftProvider.LiftProviderGroup> contraptionGroups = trackForces
                  ? BlockSubLevelLiftProvider.groupLiftProviders(contraptionProviders)
                  : List.of();

               for (BlockSubLevelLiftProvider.LiftProviderContext context : contraptionProviders) {
                  BlockSubLevelLiftProvider.LiftProviderGroup group = null;

                  for (BlockSubLevelLiftProvider.LiftProviderGroup gx : contraptionGroups) {
                     if (gx.positions().contains(context.pos())) {
                        group = gx;
                        break;
                     }
                  }

                  ((BlockSubLevelLiftProvider)context.state().getBlock())
                     .sable$contributeLiftAndDrag(
                        context, this, localContraptionPosex, timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse, group
                     );
               }

               for (BlockSubLevelLiftProvider.LiftProviderGroup group : contraptionGroups) {
                  if (group.totalLift().lengthSquared() >= 1.0E-6) {
                     this.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.LIFT.get())
                        .recordPointForce(group.liftCenter().div(group.totalLiftStrength), group.totalLift());
                  }

                  if (group.totalDrag().lengthSquared() >= 1.0E-6) {
                     this.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.DRAG.get())
                        .recordPointForce(group.dragCenter().div(group.totalDragStrength), group.totalDrag());
                  }
               }
            }
         }

         linearVelocity.fma(-0.47619047619047616 * timeStep, DimensionPhysicsData.getGravity(this.getLevel()));
         this.floatingBlockController
            .physicsTick(physicsSystem.getPartialPhysicsTick(), timeStep, linearVelocity, angularVelocity, linearImpulse, angularImpulse);
         this.reactionWheelManager.physicsTick(handle);
         handle.applyLinearAndAngularImpulse(linearImpulse, angularImpulse, false);
      }
   }

   public QueuedForceGroup getOrCreateQueuedForceGroup(ForceGroup forceGroup) {
      if (this.queuedForceGroups == null) {
         this.queuedForceGroups = new Object2ObjectOpenHashMap();
      }

      return (QueuedForceGroup)this.queuedForceGroups.computeIfAbsent(forceGroup, fg -> new QueuedForceGroup(this));
   }

   public void deleteAllEntities() {
      this.getPlot().kickAllEntities();
   }

   @Override
   public void setName(@Nullable String name) {
      if (!Objects.equals(name, this.getName())) {
         this.playerSink().sendPacket(new CustomPacketPayload[]{new ClientboundChangeSubLevelNamePacket(this.getUniqueId(), name)});
      }

      super.setName(name);
   }

   public SubLevelHeatMapManager getHeatMapManager() {
      return this.heatMapManager;
   }

   public FloatingBlockController getFloatingBlockController() {
      return this.floatingBlockController;
   }

   public ReactionWheelManager getReactionWheelManager() {
      return this.reactionWheelManager;
   }

   public void setSplitFrom(ServerSubLevel containingSubLevel, Pose3d originalPose) {
      this.splitFromSubLevel = containingSubLevel.getUniqueId();
      this.splitFromPose = originalPose;
   }

   @Nullable
   public UUID getSplitFromSubLevel() {
      return this.splitFromSubLevel;
   }

   @Nullable
   public Pose3d getSplitFromPose() {
      return this.splitFromPose;
   }

   public void clearSplitFrom() {
      this.splitFromSubLevel = null;
      this.splitFromPose = null;
   }

   public ServerLevel getLevel() {
      return (ServerLevel)super.getLevel();
   }

   public ServerLevelPlot getPlot() {
      return (ServerLevelPlot)super.getPlot();
   }

   @Override
   public MassData getMassTracker() {
      return this.massTracker;
   }

   @Internal
   public void buildMassTracker() {
      MassTracker internalTracker = MassTracker.build(new LevelAccelerator(this.getLevel()), this.getPlot().getBoundingBox());
      this.massTracker = new MergedMassTracker(this, internalTracker);
   }

   public MassTracker getSelfMassTracker() {
      return this.massTracker.getSelfMassTracker();
   }

   @Internal
   public GlobalSavedSubLevelPointer getLastSerializationPointer() {
      return this.lastSerializationPointer;
   }

   @Internal
   public void setLastSerializationPointer(GlobalSavedSubLevelPointer lastSerializationPointer) {
      this.lastSerializationPointer = lastSerializationPointer;
   }

   public void enableIndividualQueuedForcesTracking(boolean enable) {
      this.trackIndividualQueuedForces = enable;
   }

   public boolean isTrackingIndividualQueuedForces() {
      return this.trackIndividualQueuedForces;
   }

   @Nullable
   public Object2ObjectMap<ForceGroup, QueuedForceGroup> getQueuedForceGroups() {
      return this.queuedForceGroups;
   }

   @Nullable
   public CompoundTag getUserDataTag() {
      return this.userDataTag;
   }

   public void setUserDataTag(CompoundTag userDataTag) {
      this.userDataTag = userDataTag;
   }

   @Override
   public String toString() {
      return "ServerSubLevel" + super.toString();
   }
}
