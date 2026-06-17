package dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;

public class VanillaSubLevelBlockEntityRenderer implements SubLevelRenderDispatcher.BlockEntityRenderer {
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final RenderBuffers renderBuffers;
   private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

   public VanillaSubLevelBlockEntityRenderer(
      BlockEntityRenderDispatcher blockEntityRenderDispatcher,
      RenderBuffers renderBuffers,
      Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress
   ) {
      this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
      this.renderBuffers = renderBuffers;
      this.destructionProgress = destructionProgress;
   }

   @Override
   public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   @Override
   public void renderSingleBE(BlockEntity blockEntity, PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ) {
      BlockPos pos = blockEntity.getBlockPos();
      MultiBufferSource source = this.renderBuffers.bufferSource();
      poseStack.pushPose();
      poseStack.translate((double)pos.getX() - cameraX, (double)pos.getY() - cameraY, (double)pos.getZ() - cameraZ);
      SortedSet<BlockDestructionProgress> destructionProgresses = (SortedSet<BlockDestructionProgress>)this.destructionProgress.get(pos.asLong());
      if (destructionProgresses != null && !destructionProgresses.isEmpty()) {
         int progress = destructionProgresses.last().getProgress();
         if (progress >= 0) {
            Pose posestack$pose = poseStack.last();
            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
               this.renderBuffers.crumblingBufferSource().getBuffer((RenderType)ModelBakery.DESTROY_TYPES.get(progress)), posestack$pose, 1.0F
            );
            source = type -> {
               VertexConsumer consumer = this.renderBuffers.bufferSource().getBuffer(type);
               return type.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, consumer) : consumer;
            };
         }
      }

      this.blockEntityRenderDispatcher.render(blockEntity, partialTick, poseStack, source);
      poseStack.popPose();
   }
}
