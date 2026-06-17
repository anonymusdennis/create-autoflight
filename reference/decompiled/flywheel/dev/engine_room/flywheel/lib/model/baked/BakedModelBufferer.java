package dev.engine_room.flywheel.lib.model.baked;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

final class BakedModelBufferer {
   private static final ThreadLocal<BakedModelBufferer.ThreadLocalObjects> THREAD_LOCAL_OBJECTS = ThreadLocal.withInitial(
      BakedModelBufferer.ThreadLocalObjects::new
   );

   private BakedModelBufferer() {
   }

   public static SimpleModel bufferModel(
      BakedModel model, BlockPos pos, BlockAndTintGetter level, BlockState state, @Nullable PoseStack poseStack, BlockMaterialFunction blockMaterialFunction
   ) {
      BakedModelBufferer.ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
      if (poseStack == null) {
         poseStack = objects.identityPoseStack;
      }

      RandomSource random = objects.random;
      MeshEmitterManager<NeoforgeMeshEmitter> emitters = objects.emitters;
      emitters.prepare(blockMaterialFunction);
      ModelBlockRenderer blockRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
      long seed = state.getSeed(pos);
      ModelData modelData = model.getModelData(level, pos, state, level.getModelData(pos));
      random.setSeed(seed);
      ChunkRenderTypeSet renderTypes = model.getRenderTypes(state, random, modelData);
      boolean defaultAo = state.getLightEmission(level, pos) == 0;
      boolean aoEnabled = Minecraft.useAmbientOcclusion();

      for (RenderType renderType : renderTypes) {
         TriState useAo = model.useAmbientOcclusion(state, modelData, renderType);
         boolean defaultAoLayer = aoEnabled && (useAo.isTrue() || useAo.isDefault() && defaultAo);
         NeoforgeMeshEmitter emitter = emitters.getEmitter(renderType);
         emitter.prepareForModelLayer(defaultAoLayer);
         poseStack.pushPose();
         blockRenderer.tesselateBlock(level, model, state, pos, poseStack, emitter, false, random, seed, OverlayTexture.NO_OVERLAY, modelData, renderType);
         poseStack.popPose();
      }

      return emitters.end();
   }

   public static SimpleModel bufferBlocks(
      Iterator<BlockPos> posIterator,
      BlockAndTintGetter level,
      @Nullable PoseStack poseStack,
      boolean renderFluids,
      BlockMaterialFunction blockMaterialFunction
   ) {
      BakedModelBufferer.ThreadLocalObjects objects = THREAD_LOCAL_OBJECTS.get();
      if (poseStack == null) {
         poseStack = objects.identityPoseStack;
      }

      RandomSource random = objects.random;
      MeshEmitterManager<NeoforgeMeshEmitter> emitters = objects.emitters;
      TransformingVertexConsumer transformingWrapper = objects.transformingWrapper;
      emitters.prepare(blockMaterialFunction);
      BlockRenderDispatcher renderDispatcher = Minecraft.getInstance().getBlockRenderer();
      ModelBlockRenderer blockRenderer = renderDispatcher.getModelRenderer();
      ModelBlockRenderer.enableCaching();
      boolean aoEnabled = Minecraft.useAmbientOcclusion();

      while (posIterator.hasNext()) {
         BlockPos pos = posIterator.next();
         BlockState state = level.getBlockState(pos);
         emitters.prepareForBlock();
         if (renderFluids) {
            FluidState fluidState = state.getFluidState();
            if (!fluidState.isEmpty()) {
               RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
               BufferBuilder bufferBuilder = emitters.getBuffer(renderType, true, false);
               if (bufferBuilder != null) {
                  transformingWrapper.prepare(bufferBuilder, poseStack);
                  poseStack.pushPose();
                  poseStack.translate((float)(pos.getX() - (pos.getX() & 15)), (float)(pos.getY() - (pos.getY() & 15)), (float)(pos.getZ() - (pos.getZ() & 15)));
                  renderDispatcher.renderLiquid(pos, level, transformingWrapper, state, fluidState);
                  poseStack.popPose();
               }
            }
         }

         if (state.getRenderShape() == RenderShape.MODEL) {
            long seed = state.getSeed(pos);
            BakedModel model = renderDispatcher.getBlockModel(state);
            ModelData modelData = model.getModelData(level, pos, state, level.getModelData(pos));
            random.setSeed(seed);
            ChunkRenderTypeSet renderTypes = model.getRenderTypes(state, random, modelData);
            boolean defaultAo = state.getLightEmission(level, pos) == 0;

            for (RenderType renderType : renderTypes) {
               TriState useAo = model.useAmbientOcclusion(state, modelData, renderType);
               boolean defaultAoLayer = aoEnabled && (useAo.isTrue() || useAo.isDefault() && defaultAo);
               NeoforgeMeshEmitter emitter = emitters.getEmitter(renderType);
               emitter.prepareForModelLayer(defaultAoLayer);
               poseStack.pushPose();
               poseStack.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
               blockRenderer.tesselateBlock(level, model, state, pos, poseStack, emitter, true, random, seed, OverlayTexture.NO_OVERLAY, modelData, renderType);
               poseStack.popPose();
            }
         }
      }

      ModelBlockRenderer.clearCache();
      transformingWrapper.clear();
      return emitters.end();
   }

   private static class ThreadLocalObjects {
      public final PoseStack identityPoseStack = new PoseStack();
      public final RandomSource random = RandomSource.createNewThreadLocalInstance();
      public final MeshEmitterManager<NeoforgeMeshEmitter> emitters = new MeshEmitterManager<>(NeoforgeMeshEmitter::new);
      public final TransformingVertexConsumer transformingWrapper = new TransformingVertexConsumer();
   }
}
