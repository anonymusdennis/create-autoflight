package dev.engine_room.flywheel.impl.visualization;

import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.backend.RenderContext;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualManager;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationLevel;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.backend.engine.EngineImpl;
import dev.engine_room.flywheel.impl.FlwConfig;
import dev.engine_room.flywheel.impl.extension.LevelExtension;
import dev.engine_room.flywheel.impl.task.Flag;
import dev.engine_room.flywheel.impl.task.FlwTaskExecutor;
import dev.engine_room.flywheel.impl.task.RaisePlan;
import dev.engine_room.flywheel.impl.task.TaskExecutorImpl;
import dev.engine_room.flywheel.impl.visual.BandedPrimeLimiter;
import dev.engine_room.flywheel.impl.visual.DistanceUpdateLimiterImpl;
import dev.engine_room.flywheel.impl.visual.DynamicVisualContextImpl;
import dev.engine_room.flywheel.impl.visual.NonLimiter;
import dev.engine_room.flywheel.impl.visual.TickableVisualContextImpl;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;
import dev.engine_room.flywheel.impl.visualization.storage.EffectStorage;
import dev.engine_room.flywheel.impl.visualization.storage.EntityStorage;
import dev.engine_room.flywheel.lib.task.IfElsePlan;
import dev.engine_room.flywheel.lib.task.MapContextPlan;
import dev.engine_room.flywheel.lib.task.NestedPlan;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import dev.engine_room.flywheel.lib.util.LevelAttached;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

public class VisualizationManagerImpl implements VisualizationManager {
   private static final LevelAttached<VisualizationManagerImpl> MANAGERS = new LevelAttached<>(VisualizationManagerImpl::new, VisualizationManagerImpl::delete);
   private final TaskExecutorImpl taskExecutor;
   private final DistanceUpdateLimiterImpl frameLimiter;
   private final VisualizationManagerImpl.RenderDispatcherImpl renderDispatcher = new VisualizationManagerImpl.RenderDispatcherImpl();
   private final LevelAccessor level;
   @Nullable
   private VisualizationManagerImpl.LateInit lateInit;
   private final VisualManagerImpl<BlockEntity, BlockEntityStorage> blockEntities;
   private final VisualManagerImpl<Entity, EntityStorage> entities;
   private final VisualManagerImpl<Effect, EffectStorage> effects;
   private final Flag frameFlag = new Flag("frame");
   private final Flag tickFlag = new Flag("tick");

   private VisualizationManagerImpl(LevelAccessor level) {
      this.level = level;
      this.taskExecutor = FlwTaskExecutor.get();
      this.frameLimiter = this.createUpdateLimiter();
      this.blockEntities = new VisualManagerImpl<>(new BlockEntityStorage());
      this.entities = new VisualManagerImpl<>(new EntityStorage());
      this.effects = new VisualManagerImpl<>(new EffectStorage());
      if (level instanceof Level l) {
         LevelExtension.getAllLoadedEntities(l).forEach(this.entities::queueAdd);
      }
   }

   private VisualizationManagerImpl.LateInit lateInit() {
      if (this.lateInit == null) {
         this.lateInit = new VisualizationManagerImpl.LateInit(this.level);
      }

      return this.lateInit;
   }

   private DistanceUpdateLimiterImpl createUpdateLimiter() {
      return (DistanceUpdateLimiterImpl)(FlwConfig.INSTANCE.limitUpdates() ? new BandedPrimeLimiter() : new NonLimiter());
   }

   @Contract("null -> false")
   public static boolean supportsVisualization(@Nullable LevelAccessor level) {
      if (!BackendManager.isBackendOn()) {
         return false;
      } else if (level == null) {
         return false;
      } else if (!level.isClientSide()) {
         return false;
      } else {
         if (level instanceof VisualizationLevel flywheelLevel && flywheelLevel.supportsVisualization()) {
            return true;
         }

         return level == Minecraft.getInstance().level;
      }
   }

   @Nullable
   public static VisualizationManagerImpl get(@Nullable LevelAccessor level) {
      return !supportsVisualization(level) ? null : MANAGERS.get(level);
   }

   public static VisualizationManagerImpl getOrThrow(@Nullable LevelAccessor level) {
      if (!supportsVisualization(level)) {
         throw new IllegalStateException("Cannot retrieve visualization manager when visualization is not supported by level '" + level + "'!");
      } else {
         return MANAGERS.get(level);
      }
   }

   public static void reset(LevelAccessor level) {
      MANAGERS.remove(level);
   }

   public static void resetAll() {
      MANAGERS.reset();
   }

   @Override
   public Vec3i renderOrigin() {
      return this.lateInit == null ? Vec3i.ZERO : this.lateInit.engine.renderOrigin();
   }

   @Override
   public VisualManager<BlockEntity> blockEntities() {
      return this.blockEntities;
   }

   @Override
   public VisualManager<Entity> entities() {
      return this.entities;
   }

   @Override
   public VisualManager<Effect> effects() {
      return this.effects;
   }

   @Override
   public VisualizationManager.RenderDispatcher renderDispatcher() {
      return this.renderDispatcher;
   }

   public void tick() {
      this.taskExecutor.syncUntil(this.frameFlag::isRaised);
      this.frameFlag.lower();
      this.taskExecutor.syncUntil(this.tickFlag::isRaised);
      this.tickFlag.lower();
      this.lateInit().tickPlan.execute(this.taskExecutor, TickableVisualContextImpl.INSTANCE);
   }

   private void beginFrame(RenderContext context) {
      this.taskExecutor.syncUntil(this.tickFlag::isRaised);
      this.frameFlag.lower();
      this.frameLimiter.tick();
      this.lateInit().framePlan.execute(this.taskExecutor, context);
   }

   private void render(RenderContext context) {
      this.taskExecutor.syncUntil(this.frameFlag::isRaised);
      this.lateInit().engine.render(context);
   }

   private void renderCrumbling(RenderContext context, Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress) {
      if (!destructionProgress.isEmpty()) {
         List<Engine.CrumblingBlock> crumblingBlocks = new ArrayList<>();
         ObjectIterator var4 = destructionProgress.long2ObjectEntrySet().iterator();

         while (var4.hasNext()) {
            Entry<SortedSet<BlockDestructionProgress>> entry = (Entry<SortedSet<BlockDestructionProgress>>)var4.next();
            SortedSet<BlockDestructionProgress> set = (SortedSet<BlockDestructionProgress>)entry.getValue();
            if (set != null && !set.isEmpty()) {
               BlockEntityVisual<?> visual = this.blockEntities.getStorage().visualAtPos(entry.getLongKey());
               if (visual != null) {
                  List<Instance> instances = new ArrayList<>();
                  visual.collectCrumblingInstances(instance -> {
                     if (instance != null) {
                        instances.add(instance);
                     }
                  });
                  if (!instances.isEmpty()) {
                     BlockDestructionProgress maxDestruction = set.last();
                     crumblingBlocks.add(new VisualizationManagerImpl.CrumblingBlockImpl(maxDestruction.getPos(), maxDestruction.getProgress(), instances));
                  }
               }
            }
         }

         if (!crumblingBlocks.isEmpty()) {
            this.lateInit().engine.renderCrumbling(context, crumblingBlocks);
         }
      }
   }

   public void onLightUpdate(SectionPos sectionPos, LightLayer layer) {
      this.lateInit().engine.onLightUpdate(sectionPos, layer);
      long longPos = sectionPos.asLong();
      this.blockEntities.onLightUpdate(longPos);
      this.entities.onLightUpdate(longPos);
      this.effects.onLightUpdate(longPos);
   }

   private void delete() {
      this.taskExecutor.syncPoint();
      this.blockEntities.invalidate();
      this.entities.invalidate();
      this.effects.invalidate();
      if (this.lateInit != null) {
         this.lateInit.engine.delete();
      }
   }

   @Nullable
   public EngineImpl getEngineImpl() {
      if (this.lateInit == null) {
         return null;
      } else {
         Engine engine = this.lateInit.engine;
         return engine instanceof EngineImpl ? (EngineImpl)engine : null;
      }
   }

   private static record CrumblingBlockImpl(BlockPos pos, int progress, List<Instance> instances) implements Engine.CrumblingBlock {
   }

   private class LateInit {
      private final Engine engine;
      private final Plan<RenderContext> framePlan;
      private final Plan<TickableVisual.Context> tickPlan;

      private LateInit(LevelAccessor level) {
         this.engine = BackendManager.currentBackend().createEngine(level);
         VisualizationContext visualizationContext = this.engine.createVisualizationContext();
         SimplePlan<RenderContext> recreate = SimplePlan.of(
            context -> VisualizationManagerImpl.this.blockEntities.getStorage().recreateAll(visualizationContext, context.partialTick()),
            context -> VisualizationManagerImpl.this.entities.getStorage().recreateAll(visualizationContext, context.partialTick()),
            context -> VisualizationManagerImpl.this.effects.getStorage().recreateAll(visualizationContext, context.partialTick())
         );
         MapContextPlan<RenderContext, DynamicVisual.Context> update = MapContextPlan.map(this::createVisualFrameContext)
            .to(
               NestedPlan.of(
                  VisualizationManagerImpl.this.blockEntities.framePlan(visualizationContext),
                  VisualizationManagerImpl.this.entities.framePlan(visualizationContext),
                  VisualizationManagerImpl.this.effects.framePlan(visualizationContext)
               )
            );
         this.framePlan = IfElsePlan.<RenderContext>on(ctx -> this.engine.updateRenderOrigin(ctx.camera()))
            .ifTrue(recreate)
            .ifFalse(update)
            .plan()
            .then(
               SimplePlan.of(
                  () -> {
                     if (VisualizationManagerImpl.this.blockEntities.areGpuLightSectionsDirty()
                        || VisualizationManagerImpl.this.entities.areGpuLightSectionsDirty()
                        || VisualizationManagerImpl.this.effects.areGpuLightSectionsDirty()) {
                        LongOpenHashSet out = new LongOpenHashSet();
                        out.addAll(VisualizationManagerImpl.this.blockEntities.gpuLightSections());
                        out.addAll(VisualizationManagerImpl.this.entities.gpuLightSections());
                        out.addAll(VisualizationManagerImpl.this.effects.gpuLightSections());
                        this.engine.lightSections(out);
                     }
                  }
               )
            )
            .then(this.engine.createFramePlan())
            .then(RaisePlan.raise(VisualizationManagerImpl.this.frameFlag));
         this.tickPlan = NestedPlan.<TickableVisual.Context>of(
               VisualizationManagerImpl.this.blockEntities.tickPlan(visualizationContext),
               VisualizationManagerImpl.this.entities.tickPlan(visualizationContext),
               VisualizationManagerImpl.this.effects.tickPlan(visualizationContext)
            )
            .then(RaisePlan.raise(VisualizationManagerImpl.this.tickFlag));
      }

      private DynamicVisual.Context createVisualFrameContext(RenderContext ctx) {
         Vec3i renderOrigin = this.engine.renderOrigin();
         Vec3 cameraPos = ctx.camera().getPosition();
         Matrix4f viewProjection = new Matrix4f(ctx.viewProjection());
         viewProjection.translate(
            (float)((double)renderOrigin.getX() - cameraPos.x),
            (float)((double)renderOrigin.getY() - cameraPos.y),
            (float)((double)renderOrigin.getZ() - cameraPos.z)
         );
         FrustumIntersection frustum = new FrustumIntersection(viewProjection);
         return new DynamicVisualContextImpl(ctx.camera(), frustum, ctx.partialTick(), VisualizationManagerImpl.this.frameLimiter);
      }
   }

   private class RenderDispatcherImpl implements VisualizationManager.RenderDispatcher {
      @Override
      public void onStartLevelRender(RenderContext ctx) {
         VisualizationManagerImpl.this.beginFrame(ctx);
      }

      @Override
      public void afterEntities(RenderContext ctx) {
         VisualizationManagerImpl.this.render(ctx);
      }

      @Override
      public void beforeCrumbling(RenderContext ctx, Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress) {
         VisualizationManagerImpl.this.renderCrumbling(ctx, destructionProgress);
      }
   }
}
