package com.simibubi.create.content.contraptions.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.utility.worldWrappers.WrappedBlockAndTintGetter;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visual.Visual;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualEmbedding;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.baked.BlockModelBuilder;
import dev.engine_room.flywheel.lib.task.ForEachPlan;
import dev.engine_room.flywheel.lib.task.NestedPlan;
import dev.engine_room.flywheel.lib.task.PlanMap;
import dev.engine_room.flywheel.lib.task.RunnablePlan;
import dev.engine_room.flywheel.lib.visual.AbstractEntityVisual;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.MutablePair;

public class ContraptionVisual<E extends AbstractContraptionEntity> extends AbstractEntityVisual<E> implements DynamicVisual, TickableVisual, ShaderLightVisual {
   protected static final int DEFAULT_LIGHT_PADDING = 1;
   protected final VisualEmbedding embedding;
   protected final List<BlockEntityVisual<?>> children = new ArrayList<>();
   protected final List<ActorVisual> actors = new ArrayList<>();
   protected final PlanMap<DynamicVisual, Context> dynamicVisuals = new PlanMap();
   protected final PlanMap<TickableVisual, dev.engine_room.flywheel.api.visual.TickableVisual.Context> tickableVisuals = new PlanMap();
   protected TransformedInstance structure;
   protected SectionCollector sectionCollector;
   protected long minSection;
   protected long maxSection;
   protected int lightPaddingBlocks = 1;
   protected int lastStructureVersion;
   protected int lastVersionChildren;
   private final PoseStack contraptionMatrix = new PoseStack();

   public ContraptionVisual(VisualizationContext ctx, E entity, float partialTick) {
      super(ctx, entity, partialTick);
      this.embedding = ctx.createEmbedding(Vec3i.ZERO);
      this.setEmbeddingMatrices(partialTick);
      Contraption contraption = entity.getContraption();
      if (contraption != null) {
         ClientContraption clientContraption = contraption.getOrCreateClientContraptionLazy();
         this.setupStructure(clientContraption);
         this.setupChildren(contraption, clientContraption, partialTick);
      }
   }

   private void setupStructure(ClientContraption clientContraption) {
      VirtualRenderWorld renderLevel = clientContraption.getRenderLevel();
      final ClientContraption.RenderedBlocks blocks = clientContraption.getRenderedBlocks();
      BlockAndTintGetter modelWorld = new WrappedBlockAndTintGetter(renderLevel) {
         @Override
         public BlockState getBlockState(BlockPos pos) {
            return blocks.lookup().apply(pos);
         }
      };
      SimpleModel model = new BlockModelBuilder(modelWorld, blocks.positions())
         .materialFunc(
            (renderType, shaded, ao) -> {
               Material material = ModelUtil.getMaterial(renderType, shaded, ao);
               return (Material)(material != null && material.cardinalLightingMode() == CardinalLightingMode.ENTITY
                  ? SimpleMaterial.builderOf(material).cardinalLightingMode(CardinalLightingMode.CHUNK).build()
                  : material);
            }
         )
         .build();
      Instancer<TransformedInstance> instancer = this.embedding.instancerProvider().instancer(InstanceTypes.TRANSFORMED, model);
      if (this.structure == null) {
         this.structure = (TransformedInstance)instancer.createInstance();
      } else {
         instancer.stealInstance(this.structure);
      }

      this.structure.setChanged();
      this.lastStructureVersion = clientContraption.structureVersion();
   }

   private void setupChildren(Contraption contraption, ClientContraption clientContraption, float partialTick) {
      this.children.forEach(Visual::delete);
      this.children.clear();
      this.dynamicVisuals.clear();
      this.tickableVisuals.clear();

      for (BlockEntity be : clientContraption.renderedBlockEntityView) {
         this.setupVisualizer(be, partialTick);
      }

      VirtualRenderWorld renderLevel = clientContraption.getRenderLevel();
      this.actors.forEach(ActorVisual::delete);
      this.actors.clear();

      for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
         this.setupActor(actor, renderLevel);
      }

      this.lastVersionChildren = clientContraption.childrenVersion();
   }

   protected <T extends BlockEntity> void setupVisualizer(T be, float partialTicks) {
      BlockEntityVisualizer<? super T> visualizer = VisualizerRegistry.getVisualizer(be.getType());
      if (visualizer != null) {
         BlockEntityVisual<? super T> visual = visualizer.createVisual(this.embedding, be, partialTicks);
         this.children.add(visual);
         if (visual instanceof DynamicVisual dynamic) {
            this.dynamicVisuals.add(dynamic, dynamic.planFrame());
         }

         if (visual instanceof TickableVisual tickable) {
            this.tickableVisuals.add(tickable, tickable.planTick());
         }
      }
   }

   protected void setupActor(MutablePair<StructureBlockInfo, MovementContext> actor, VirtualRenderWorld renderLevel) {
      MovementContext context = (MovementContext)actor.getRight();
      if (context != null) {
         if (context.world == null) {
            context.world = this.level;
         }

         StructureBlockInfo blockInfo = (StructureBlockInfo)actor.getLeft();
         MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(blockInfo.state());
         if (movementBehaviour != null) {
            ActorVisual visual = movementBehaviour.createVisual(this.embedding, renderLevel, context);
            if (visual != null) {
               this.actors.add(visual);
            }
         }
      }
   }

   public Plan<dev.engine_room.flywheel.api.visual.TickableVisual.Context> planTick() {
      return NestedPlan.of(new Plan[]{ForEachPlan.of(() -> this.actors, ActorVisual::tick), this.tickableVisuals});
   }

   public Plan<Context> planFrame() {
      return RunnablePlan.of(this::beginFrame).then(NestedPlan.of(new Plan[]{ForEachPlan.of(() -> this.actors, ActorVisual::beginFrame), this.dynamicVisuals}));
   }

   protected void beginFrame(Context context) {
      float partialTick = context.partialTick();
      this.setEmbeddingMatrices(partialTick);
      this.checkAndUpdateLightSections();
      Contraption contraption = ((AbstractContraptionEntity)this.entity).getContraption();
      ClientContraption clientContraption = contraption.getOrCreateClientContraptionLazy();
      if (this.lastStructureVersion != clientContraption.structureVersion()) {
         this.setupStructure(clientContraption);
      }

      if (this.lastVersionChildren != clientContraption.childrenVersion()) {
         this.setupChildren(contraption, clientContraption, partialTick);
      }
   }

   private void setEmbeddingMatrices(float partialTick) {
      Vec3i origin = this.renderOrigin();
      double x;
      double y;
      double z;
      if (((AbstractContraptionEntity)this.entity).isPrevPosInvalid()) {
         x = ((AbstractContraptionEntity)this.entity).getX() - (double)origin.getX();
         y = ((AbstractContraptionEntity)this.entity).getY() - (double)origin.getY();
         z = ((AbstractContraptionEntity)this.entity).getZ() - (double)origin.getZ();
      } else {
         x = Mth.lerp((double)partialTick, ((AbstractContraptionEntity)this.entity).xo, ((AbstractContraptionEntity)this.entity).getX())
            - (double)origin.getX();
         y = Mth.lerp((double)partialTick, ((AbstractContraptionEntity)this.entity).yo, ((AbstractContraptionEntity)this.entity).getY())
            - (double)origin.getY();
         z = Mth.lerp((double)partialTick, ((AbstractContraptionEntity)this.entity).zo, ((AbstractContraptionEntity)this.entity).getZ())
            - (double)origin.getZ();
      }

      this.contraptionMatrix.setIdentity();
      this.contraptionMatrix.translate(x, y, z);
      ((AbstractContraptionEntity)this.entity).applyLocalTransforms(this.contraptionMatrix, partialTick);
      this.embedding.transforms(this.contraptionMatrix.last().pose(), this.contraptionMatrix.last().normal());
   }

   public void setSectionCollector(SectionCollector collector) {
      this.sectionCollector = collector;
      this.checkAndUpdateLightSections();
   }

   private void checkAndUpdateLightSections() {
      AABB boundingBox = ((AbstractContraptionEntity)this.entity).getBoundingBox();
      int minSectionX = SectionPos.blockToSectionCoord(Mth.floor(boundingBox.minX) - this.lightPaddingBlocks);
      int minSectionY = SectionPos.blockToSectionCoord(Mth.floor(boundingBox.minY) - this.lightPaddingBlocks);
      int minSectionZ = SectionPos.blockToSectionCoord(Mth.floor(boundingBox.minZ) - this.lightPaddingBlocks);
      int maxSectionX = SectionPos.blockToSectionCoord(Mth.ceil(boundingBox.maxX) + this.lightPaddingBlocks);
      int maxSectionY = SectionPos.blockToSectionCoord(Mth.ceil(boundingBox.maxY) + this.lightPaddingBlocks);
      int maxSectionZ = SectionPos.blockToSectionCoord(Mth.ceil(boundingBox.maxZ) + this.lightPaddingBlocks);
      if (this.minSection != SectionPos.asLong(minSectionX, minSectionY, minSectionZ)
         || this.maxSection != SectionPos.asLong(maxSectionX, maxSectionY, maxSectionZ)) {
         this.minSection = SectionPos.asLong(minSectionX, minSectionY, minSectionZ);
         this.maxSection = SectionPos.asLong(maxSectionX, maxSectionY, maxSectionZ);
         LongSet longSet = new LongArraySet();

         for (int x = minSectionX; x <= maxSectionX; x++) {
            for (int y = minSectionY; y <= maxSectionY; y++) {
               for (int z = minSectionZ; z <= maxSectionZ; z++) {
                  longSet.add(SectionPos.asLong(x, y, z));
               }
            }
         }

         this.sectionCollector.sections(longSet);
      }
   }

   protected void _delete() {
      this.children.forEach(Visual::delete);
      this.actors.forEach(ActorVisual::delete);
      if (this.structure != null) {
         this.structure.delete();
      }

      this.embedding.delete();
   }
}
