package dev.ryanhcode.sable.sublevel.system;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineProvider;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.api.physics.object.ArbitraryPhysicsObject;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.mixinterface.toast.SableToastableServer;
import dev.ryanhcode.sable.physics.config.PhysicsConfigData;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.platform.SableEventPublishPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.ticket.PhysicsChunkTicketManager;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public class SubLevelPhysicsSystem implements SubLevelObserver {
   public static final int DEFAULT_RESIDENT_CAPACITY = 8;
   public static final boolean USE_TICKETS_FOR_QUERIES = false;
   public static boolean IN_PHYSICS_STEP = false;
   public static SubLevelPhysicsSystem currentlySteppingSystem;
   private final PhysicsPipeline pipeline;
   private final ServerLevel level;
   private final Object2IntMap<UUID> punchCooldowns = new Object2IntOpenHashMap();
   private final PhysicsConfigData config = new PhysicsConfigData();
   private final PhysicsChunkTicketManager ticketManager = new PhysicsChunkTicketManager();
   private final Pose3d storagePose = new Pose3d();
   private final Collection<ArbitraryPhysicsObject> arbitraryObjects = new ObjectOpenHashSet();
   private boolean paused;
   private int currentSubstep;

   public SubLevelPhysicsSystem(ServerLevel level) {
      this.level = level;
      Sable.LOGGER.info("Creating physics pipeline for {} using {}", level.dimension(), PhysicsPipelineProvider.INSTANCE.getClass().getSimpleName());
      this.pipeline = PhysicsPipelineProvider.INSTANCE.createPipeline(level);
   }

   public static SubLevelPhysicsSystem get(Level level) {
      return SubLevelContainer.getContainer(level) instanceof ServerSubLevelContainer serverContainer ? serverContainer.physicsSystem() : null;
   }

   @NotNull
   public static SubLevelPhysicsSystem require(Level level) {
      if (SubLevelContainer.getContainer(level) instanceof ServerSubLevelContainer serverContainer) {
         return Objects.requireNonNull(serverContainer.physicsSystem());
      } else {
         throw new IllegalArgumentException("Sub-level container not found");
      }
   }

   public static SubLevelPhysicsSystem getCurrentlySteppingSystem() {
      if (currentlySteppingSystem == null) {
         throw new IllegalStateException("No physics system is currently stepping");
      } else {
         return currentlySteppingSystem;
      }
   }

   public void initialize() {
      Vector3d gravity = new Vector3d(DimensionPhysicsData.getGravity(this.level));
      double universalDrag = DimensionPhysicsData.getUniversalDrag(this.level);
      this.pipeline.init(gravity, universalDrag);
      this.pipeline.updateConfigFrom(this.config);
   }

   public void onConfigUpdated() {
      this.pipeline.updateConfigFrom(this.config);
   }

   @Override
   public void onSubLevelAdded(SubLevel subLevel) {
      if (subLevel instanceof ServerSubLevel serverSubLevel) {
         serverSubLevel.buildMassTracker();
         this.pipeline.add(serverSubLevel, serverSubLevel.logicalPose());
      } else {
         throw new UnsupportedOperationException("Client sub-levels are not supported by the physics system. How did we end up here?");
      }
   }

   @Override
   public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
      if (subLevel instanceof ServerSubLevel serverSubLevel) {
         this.pipeline.remove(serverSubLevel);
      } else {
         throw new UnsupportedOperationException("Client sub-levels are not supported by the physics system");
      }
   }

   @Override
   public void tick(SubLevelContainer sidelessContainer) {
      ServerSubLevelContainer container = (ServerSubLevelContainer)sidelessContainer;
      this.tickPunchCooldowns();
      this.ticketManager.update(this.level, container, this, this.pipeline, 0.05);

      for (ServerSubLevel subLevel : container.getAllSubLevels()) {
         subLevel.updateLastPose();

         for (BlockEntitySubLevelActor actor : subLevel.getPlot().getBlockEntityActors()) {
            actor.sable$tick(subLevel);
         }
      }

      this.pipeline.tick();
      if (!this.paused) {
         currentlySteppingSystem = this;

         try {
            this.tickPipelinePhysics(container);
         } catch (Exception var7) {
            CrashReport crashReport = CrashReport.forThrowable(var7, "Sable ticking physics");
            CrashReportCategory crashReportCategory = crashReport.addCategory("Current physics state");
            crashReportCategory.setDetail("Dimension", this.level.dimension());
            throw new ReportedException(crashReport);
         }

         currentlySteppingSystem = null;
      }
   }

   private void tickPipelinePhysics(ServerSubLevelContainer container) {
      this.pipeline.prePhysicsTicks();

      for (this.currentSubstep = 0; this.currentSubstep < this.config.substepsPerTick; this.currentSubstep++) {
         double substepTimeStep = 0.05 / (double)this.config.substepsPerTick;

         for (ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (!subLevel.isRemoved()) {
               subLevel.prePhysicsTickBegin();
            }
         }

         for (ServerSubLevel subLevelx : container.getAllSubLevels()) {
            if (!subLevelx.isRemoved()) {
               subLevelx.updateMergedMassData((float)this.getPartialPhysicsTick());
            }
         }

         for (ServerSubLevel subLevelxx : container.getAllSubLevels()) {
            if (!subLevelxx.isRemoved()) {
               subLevelxx.prePhysicsTick(this, this.getPhysicsHandle(subLevelxx), substepTimeStep);
            }
         }

         SableEventPublishPlatform.INSTANCE.prePhysicsTick(this, substepTimeStep);

         for (ServerSubLevel subLevelxxx : container.getAllSubLevels()) {
            if (!subLevelxxx.isRemoved()) {
               subLevelxxx.applyQueuedForces(this, this.getPhysicsHandle(subLevelxxx), substepTimeStep);
            }
         }

         IN_PHYSICS_STEP = true;
         this.pipeline.physicsTick(substepTimeStep);
         IN_PHYSICS_STEP = false;
         container.processSubLevelRemovals();
         this.updateAllPoses(container);
         SableEventPublishPlatform.INSTANCE.postPhysicsTick(this, substepTimeStep);
      }

      this.pipeline.postPhysicsTicks();
      this.currentSubstep = this.config.substepsPerTick;
   }

   private void updateAllPoses(ServerSubLevelContainer container) {
      for (ServerSubLevel subLevel : container.getAllSubLevels()) {
         if (!subLevel.isRemoved()) {
            this.updatePose(subLevel);
         }
      }
   }

   public void updatePose(ServerSubLevel serverSubLevel) {
      this.pipeline.readPose(serverSubLevel, this.storagePose);
      Vector3d position = this.storagePose.position();
      Quaterniond orientation = this.storagePose.orientation();
      if (Double.isNaN(position.x)
         || Double.isNaN(position.y)
         || Double.isNaN(position.z)
         || Double.isNaN(orientation.x)
         || Double.isNaN(orientation.y)
         || Double.isNaN(orientation.z)
         || Double.isNaN(orientation.w)) {
         Sable.LOGGER
            .info(
               "Invalid position {} or orientation {} received for sub-level {} from pipeline.",
               new Object[]{this.storagePose.position(), this.storagePose.orientation(), serverSubLevel}
            );
         if (!this.recoverSubLevel(serverSubLevel)) {
            return;
         }

         this.pipeline.readPose(serverSubLevel, this.storagePose);
      }

      Pose3d logicalPose = serverSubLevel.logicalPose();
      logicalPose.position().set(this.storagePose.position());
      logicalPose.orientation().set(this.storagePose.orientation());
      logicalPose.position().sub(serverSubLevel.lastPose().position(), serverSubLevel.latestLinearVelocity);
      orientation = logicalPose.orientation().difference(serverSubLevel.lastPose().orientation(), new Quaterniond()).conjugate();
      Vector3d angularVelocity = serverSubLevel.latestAngularVelocity.set(orientation.x, orientation.y, orientation.z);
      if (angularVelocity.lengthSquared() <= 1.0E-15) {
         angularVelocity.mul(2.0 / orientation.w);
      } else {
         angularVelocity.normalize().mul(2.0 * Math.safeAcos(orientation.w));
      }

      serverSubLevel.latestLinearVelocity.mul(20.0);
      serverSubLevel.latestAngularVelocity.mul(20.0);
   }

   public boolean recoverSubLevel(ServerSubLevel serverSubLevel) {
      Sable.LOGGER.info("Attempting to recover physics state for sub-level {}. Removing and re-adding from pipeline.", serverSubLevel);
      if (this.level.getServer() instanceof SableToastableServer toastable) {
         toastable.sable$reportSubLevelPhysicsFailure(serverSubLevel);
      }

      this.pipeline.remove(serverSubLevel);
      serverSubLevel.buildMassTracker();
      this.pipeline.add(serverSubLevel, serverSubLevel.logicalPose());
      if (serverSubLevel.getMassTracker().getCenterOfMass() == null) {
         Sable.LOGGER.info("Sub-level recovery added sub-level to pipeline, but center of mass is null. Aborting and removing sub-level.");
         SubLevelContainer.getContainer(this.level).removeSubLevel(serverSubLevel, SubLevelRemovalReason.REMOVED);
         return false;
      } else {
         ServerLevelPlot plot = serverSubLevel.getPlot();

         for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            ChunkPos global = chunk.getPos();
            LevelChunkSection[] levelChunkSections = chunk.getSections();

            for (int i = 0; i < chunk.getSectionsCount(); i++) {
               LevelChunkSection section = levelChunkSections[i];
               if (!section.hasOnlyAir()) {
                  int sectionY = chunk.getSectionYFromSectionIndex(i);
                  this.pipeline.handleChunkSectionAddition(section, global.x, sectionY, global.z, true);
               }
            }
         }

         return true;
      }
   }

   private void tickPunchCooldowns() {
      ObjectIterator<Entry<UUID>> punchCooldownIter = this.punchCooldowns.object2IntEntrySet().iterator();

      while (punchCooldownIter.hasNext()) {
         Entry<UUID> entry = (Entry<UUID>)punchCooldownIter.next();
         int cooldown = entry.getIntValue() - 1;
         if (cooldown <= 0) {
            punchCooldownIter.remove();
         } else {
            entry.setValue(cooldown);
         }
      }
   }

   public boolean tryPunch(UUID player, int cooldownAttempt) {
      int cooldown = this.punchCooldowns.getOrDefault(player, 0);
      if (cooldown > 0) {
         return false;
      } else {
         int newCooldown = Math.max(SableConfig.SUB_LEVEL_PUNCH_COOLDOWN_TICKS.getAsInt(), cooldownAttempt);
         this.punchCooldowns.put(player, newCooldown);
         return true;
      }
   }

   public PhysicsPipeline getPipeline() {
      return this.pipeline;
   }

   public RigidBodyHandle getPhysicsHandle(@NotNull ServerSubLevel subLevel) {
      return new RigidBodyHandle(Objects.requireNonNull(subLevel), this);
   }

   public void handleBlockChange(SectionPos sectionPos, LevelChunkSection section, int localX, int localY, int localZ, BlockState oldState, BlockState newState) {
      ChunkPos chunk = sectionPos.chunk();
      LevelPlot plot = ((SubLevelContainerHolder)this.level).sable$getPlotContainer().getPlot(chunk);
      if (plot != null) {
         this.ticketManager.addSectionIfNotTracked(this.level, section, sectionPos, this.pipeline);
      }

      int x = (sectionPos.x() << 4) + localX;
      int y = (sectionPos.y() << 4) + localY;
      int z = (sectionPos.z() << 4) + localZ;
      SubLevel subLevel = Sable.HELPER.getContaining(this.level, sectionPos);
      BlockPos globalBlockPos = new BlockPos(x, y, z);
      this.updateMassDataFromBlockChange(subLevel, globalBlockPos, oldState, newState, !IN_PHYSICS_STEP);
      this.pipeline.handleBlockChange(sectionPos, section, localX, localY, localZ, oldState, newState);
      this.wakeUpObjectsAt(x, y, z);
   }

   public void wakeUpObjectsAt(int x, int y, int z) {
      BoundingBox3d bounds = new BoundingBox3d((double)x, (double)y, (double)z, (double)(x + 1), (double)(y + 1), (double)(z + 1));
      bounds.expand(0.1, bounds);

      for (SubLevel intersectingSubLevel : Sable.HELPER.getAllIntersecting(this.level, bounds)) {
         if (intersectingSubLevel instanceof ServerSubLevel) {
            ServerSubLevel intersectingServerSubLevel = (ServerSubLevel)intersectingSubLevel;
            if (!intersectingServerSubLevel.isRemoved()) {
               this.pipeline.wakeUp(intersectingServerSubLevel);
            }
         }
      }

      if (!this.arbitraryObjects.isEmpty()) {
         BoundingBox3d objectBounds = new BoundingBox3d();

         for (ArbitraryPhysicsObject object : this.arbitraryObjects) {
            object.getBoundingBox(objectBounds);
            if (objectBounds.intersects(bounds)) {
               object.wakeUp();
            }
         }
      }
   }

   public void updateMassDataFromBlockChange(SubLevel subLevel, BlockPos globalBlockPos, BlockState oldState, BlockState newState, boolean notifyPipeline) {
      if (subLevel instanceof ServerSubLevel serverSubLevel) {
         Vec3 oldInertia = oldState.isAir() ? null : PhysicsBlockPropertyHelper.getInertia(this.level, globalBlockPos, oldState);
         Vec3 inertia = newState.isAir() ? null : PhysicsBlockPropertyHelper.getInertia(this.level, globalBlockPos, newState);
         double oldMass = oldState.isAir() ? 0.0 : PhysicsBlockPropertyHelper.getMass(this.level, globalBlockPos, oldState);
         double mass = newState.isAir() ? 0.0 : PhysicsBlockPropertyHelper.getMass(this.level, globalBlockPos, newState);
         if (mass != oldMass || newState != oldState && (oldMass != 0.0 || mass != 0.0) || oldInertia != inertia) {
            Level level = subLevel.getLevel();
            MassTracker massTracker = serverSubLevel.getSelfMassTracker();
            if (mass != 0.0) {
               massTracker.addBlockMass(level, newState, globalBlockPos, mass, inertia);
            }

            if (oldMass != 0.0) {
               massTracker.addBlockMass(level, oldState, globalBlockPos, -oldMass, oldInertia);
            }

            if (!subLevel.isRemoved() && massTracker.isInvalid()) {
               serverSubLevel.getPlot().destroyAllBlocks();
               serverSubLevel.markRemoved();
               return;
            }

            if (notifyPipeline) {
               serverSubLevel.updateMergedMassData((float)this.getPartialPhysicsTick());
               this.pipeline.onStatsChanged(serverSubLevel);
            }
         }
      }
   }

   public double getPartialPhysicsTick() {
      return (double)(this.currentSubstep + 1) / (double)this.config.substepsPerTick;
   }

   public boolean getPaused() {
      return this.paused;
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   public Iterable<SubLevel> queryIntersecting(BoundingBox3dc bounds) {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);

      assert container != null : "No sub-level container found for level that somehow also has a physics system";

      return container.queryIntersecting(bounds);
   }

   public PhysicsConfigData getConfig() {
      return this.config;
   }

   public Iterable<ArbitraryPhysicsObject> getArbitraryObjects() {
      return this.arbitraryObjects;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   public PhysicsChunkTicketManager getTicketManager() {
      return this.ticketManager;
   }

   public void addObject(ArbitraryPhysicsObject object) {
      if (this.arbitraryObjects.add(object)) {
         object.onAddition(this);
      }
   }

   public void removeObject(ArbitraryPhysicsObject object) {
      if (this.arbitraryObjects.remove(object)) {
         object.onRemoved();
      }
   }

   public int getNextRuntimeID() {
      return this.pipeline.getNextRuntimeID();
   }
}
