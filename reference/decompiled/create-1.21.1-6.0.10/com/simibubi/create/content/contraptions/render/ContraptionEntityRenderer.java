package com.simibubi.create.content.contraptions.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.render.BlockEntityRenderHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.BitSet;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.ShadedBlockSbbBuilder;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.createmod.catnip.render.SuperByteBufferCache.Compartment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.apache.commons.lang3.tuple.Pair;

public class ContraptionEntityRenderer<C extends AbstractContraptionEntity> extends EntityRenderer<C> {
   public static final Compartment<Pair<Contraption, RenderType>> CONTRAPTION = new Compartment();
   private static final ThreadLocal<ContraptionEntityRenderer.ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(
      ContraptionEntityRenderer.ThreadLocalObjects::new
   );

   public ContraptionEntityRenderer(Context context) {
      super(context);
   }

   public static SuperByteBuffer getBuffer(Contraption contraption, VirtualRenderWorld renderWorld, RenderType renderType) {
      return SuperByteBufferCache.getInstance()
         .get(CONTRAPTION, Pair.of(contraption, renderType), () -> buildStructureBuffer(contraption, renderWorld, renderType));
   }

   private static SuperByteBuffer buildStructureBuffer(Contraption contraption, VirtualRenderWorld renderWorld, RenderType layer) {
      BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
      ModelBlockRenderer renderer = dispatcher.getModelRenderer();
      ContraptionEntityRenderer.ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
      PoseStack poseStack = objects.poseStack;
      RandomSource random = objects.random;
      ClientContraption clientContraption = contraption.getOrCreateClientContraptionLazy();
      ClientContraption.RenderedBlocks blocks = clientContraption.getRenderedBlocks();
      ShadedBlockSbbBuilder sbbBuilder = objects.sbbBuilder;
      sbbBuilder.begin();
      ModelBlockRenderer.enableCaching();

      for (BlockPos pos : blocks.positions()) {
         BlockState state = blocks.lookup().apply(pos);
         if (state.getRenderShape() == RenderShape.MODEL) {
            BakedModel model = dispatcher.getBlockModel(state);
            ModelData modelData = renderWorld.getModelData(pos);
            modelData = model.getModelData(renderWorld, pos, state, modelData);
            long randomSeed = state.getSeed(pos);
            random.setSeed(randomSeed);
            if (model.getRenderTypes(state, random, modelData).contains(layer)) {
               poseStack.pushPose();
               poseStack.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
               renderer.tesselateBlock(
                  renderWorld, model, state, pos, poseStack, sbbBuilder, true, random, randomSeed, OverlayTexture.NO_OVERLAY, modelData, layer
               );
               poseStack.popPose();
            }
         }
      }

      ModelBlockRenderer.clearCache();
      return sbbBuilder.end();
   }

   public ResourceLocation getTextureLocation(C entity) {
      return null;
   }

   public boolean shouldRender(C entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
      if (entity.getContraption() == null) {
         return false;
      } else if (!entity.isAliveOrStale()) {
         return false;
      } else {
         return !entity.isReadyForRender() ? false : super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ);
      }
   }

   public void render(C entity, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int overlay) {
      super.render(entity, yaw, partialTicks, poseStack, buffers, overlay);
      Contraption contraption = entity.getContraption();
      if (contraption != null) {
         Level level = entity.level();
         ClientContraption clientContraption = contraption.getOrCreateClientContraptionLazy();
         VirtualRenderWorld renderWorld = clientContraption.getRenderLevel();
         ContraptionMatrices matrices = clientContraption.getMatrices();
         matrices.setup(poseStack, entity);
         if (!VisualizationManager.supportsVisualization(level)) {
            for (RenderType renderType : RenderType.chunkBufferLayers()) {
               SuperByteBuffer sbb = getBuffer(contraption, renderWorld, renderType);
               if (!sbb.isEmpty()) {
                  VertexConsumer vc = buffers.getBuffer(renderType);
                  ((SuperByteBuffer)sbb.transform(matrices.getModel())).useLevelLight(level, matrices.getWorld()).renderInto(poseStack, vc);
               }
            }
         }

         BitSet adjustRenderedBlockEntities = clientContraption.getAndAdjustShouldRenderBlockEntities();
         clientContraption.scratchErroredBlockEntities.clear();
         BlockEntityRenderHelper.renderBlockEntities(
            clientContraption.renderedBlockEntityView,
            adjustRenderedBlockEntities,
            clientContraption.scratchErroredBlockEntities,
            renderWorld,
            level,
            matrices.getModelViewProjection(),
            matrices.getLight(),
            buffers,
            AnimationTickHolder.getPartialTicks()
         );
         clientContraption.shouldRenderBlockEntities.andNot(clientContraption.scratchErroredBlockEntities);
         renderActors(level, renderWorld, contraption, matrices, buffers);
         matrices.clear();
      }
   }

   private static void renderActors(Level level, VirtualRenderWorld renderWorld, Contraption c, ContraptionMatrices matrices, MultiBufferSource buffer) {
      PoseStack m = matrices.getModel();

      for (Pair<StructureBlockInfo, MovementContext> actor : c.getActors()) {
         MovementContext context = (MovementContext)actor.getRight();
         if (context != null) {
            if (context.world == null) {
               context.world = level;
            }

            StructureBlockInfo blockInfo = (StructureBlockInfo)actor.getLeft();
            MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(blockInfo.state());
            if (movementBehaviour != null && !c.isHiddenInPortal(blockInfo.pos())) {
               m.pushPose();
               TransformStack.of(m).translate(blockInfo.pos());
               movementBehaviour.renderInContraption(context, renderWorld, matrices, buffer);
               m.popPose();
            }
         }
      }
   }

   private static class ThreadLocalObjects {
      public final PoseStack poseStack = new PoseStack();
      public final RandomSource random = RandomSource.createNewThreadLocalInstance();
      public final ShadedBlockSbbBuilder sbbBuilder = ShadedBlockSbbBuilder.create();
   }
}
