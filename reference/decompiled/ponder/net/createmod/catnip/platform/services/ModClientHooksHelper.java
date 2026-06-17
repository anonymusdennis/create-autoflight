package net.createmod.catnip.platform.services;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Iterator;
import java.util.Locale;
import net.createmod.catnip.client.render.model.ShadeSeparatedBufferSource;
import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.createmod.catnip.render.ShadedBlockSbbBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public interface ModClientHooksHelper {
   Locale getCurrentLocale();

   @Nullable
   <T extends ParticleOptions> Particle createParticleFromData(
      T var1, ClientLevel var2, double var3, double var5, double var7, double var9, double var11, double var13
   );

   Minecraft getMinecraftFromScreen(Screen var1);

   boolean isKeyPressed(KeyMapping var1);

   void enableStencilBuffer(RenderTarget var1);

   void renderFullFluidState(PoseStack var1, BufferSource var2, FluidState var3);

   @Internal
   void bufferModel(BakedModel var1, BlockPos var2, BlockAndTintGetter var3, BlockState var4, @Nullable PoseStack var5, ShadeSeparatedBufferSource var6);

   @Internal
   void bufferModel(BakedModel var1, BlockPos var2, BlockAndTintGetter var3, BlockState var4, @Nullable PoseStack var5, ShadeSeparatedResultConsumer var6);

   @Internal
   void bufferBlocks(Iterator<BlockPos> var1, BlockAndTintGetter var2, @Nullable PoseStack var3, boolean var4, ShadeSeparatedBufferSource var5);

   @Internal
   void bufferBlocks(Iterator<BlockPos> var1, BlockAndTintGetter var2, @Nullable PoseStack var3, boolean var4, ShadeSeparatedResultConsumer var5);

   @Deprecated(
      forRemoval = true
   )
   default ShadedBlockSbbBuilder createSbbBuilder(BufferBuilder builder) {
      return ShadedBlockSbbBuilder.create();
   }

   @Deprecated(
      forRemoval = true
   )
   Iterable<RenderType> getRenderTypesForBlockModel(BlockState var1, RandomSource var2, @Nullable BlockEntity var3);

   @Deprecated(
      forRemoval = true
   )
   boolean doesBlockModelContainRenderType(RenderType var1, BlockState var2, RandomSource var3, @Nullable BlockEntity var4);

   @Deprecated(
      forRemoval = true
   )
   void tesselateBlockVirtual(
      BlockRenderDispatcher var1,
      BakedModel var2,
      BlockState var3,
      BlockPos var4,
      PoseStack var5,
      VertexConsumer var6,
      boolean var7,
      RandomSource var8,
      long var9,
      int var11,
      RenderType var12
   );

   @Deprecated(
      forRemoval = true
   )
   default void tesselateBlockVirtual(
      Level level,
      BlockRenderDispatcher dispatcher,
      BakedModel model,
      BlockState state,
      BlockPos pos,
      PoseStack poseStack,
      VertexConsumer consumer,
      boolean checkSides,
      RandomSource randomSource,
      long seed,
      int packedOverlay,
      RenderType renderType
   ) {
      this.tesselateBlockVirtual(dispatcher, model, state, pos, poseStack, consumer, checkSides, randomSource, seed, packedOverlay, renderType);
   }

   @Deprecated(
      forRemoval = true
   )
   void renderGuiGameElementModel(
      BlockRenderDispatcher var1, BufferSource var2, PoseStack var3, BlockState var4, BakedModel var5, int var6, @Nullable BlockEntity var7
   );

   @Deprecated(
      forRemoval = true
   )
   default void renderGuiGameElementModel(
      BlockRenderDispatcher blockRenderer, BufferSource buffer, PoseStack ms, BlockState state, BakedModel blockModel, int color
   ) {
      this.renderGuiGameElementModel(blockRenderer, buffer, ms, state, blockModel, color, null);
   }

   @Deprecated(
      forRemoval = true
   )
   void renderVirtualBlockStateModel(
      BlockRenderDispatcher var1, PoseStack var2, VertexConsumer var3, BlockState var4, BakedModel var5, float var6, float var7, float var8, RenderType var9
   );

   @Deprecated(
      forRemoval = true
   )
   void vertexConsumerPutBulkDataWithAlpha(VertexConsumer var1, Pose var2, BakedQuad var3, float var4, float var5, float var6, float var7, int var8, int var9);

   @Deprecated(
      forRemoval = true
   )
   default BlockRenderDispatcher getBlockRenderDispatcher() {
      return Minecraft.getInstance().getBlockRenderer();
   }

   @Deprecated(
      forRemoval = true
   )
   default boolean chunkRenderTypeMatches(BlockState state, RenderType layer) {
      return ItemBlockRenderTypes.getChunkRenderType(state) == layer;
   }

   @Deprecated(
      forRemoval = true
   )
   default boolean fluidRenderTypeMatches(FluidState state, RenderType layer) {
      return ItemBlockRenderTypes.getRenderLayer(state) == layer;
   }
}
