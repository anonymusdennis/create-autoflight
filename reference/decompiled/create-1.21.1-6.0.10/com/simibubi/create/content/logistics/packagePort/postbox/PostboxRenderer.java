package com.simibubi.create.content.logistics.packagePort.postbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.Transform;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

public class PostboxRenderer extends SmartBlockEntityRenderer<PostboxBlockEntity> {
   public PostboxRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(PostboxBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (blockEntity.addressFilter != null && !blockEntity.addressFilter.isBlank()) {
         this.renderNameplateOnHover(blockEntity, Component.literal(blockEntity.addressFilter), 1.0F, ms, buffer, light);
      }

      SuperByteBuffer sbb = CachedBuffers.partial(AllPartialModels.POSTBOX_FLAG, blockEntity.getBlockState());
      sbb.light(light)
         .overlay(overlay)
         .rotateCentered((float) (Math.PI / 180.0) * (180.0F - ((Direction)blockEntity.getBlockState().getValue(PostboxBlock.FACING)).toYRot()), Axis.YP);
      transformFlag(sbb, blockEntity, partialTicks);
      sbb.renderInto(ms, buffer.getBuffer(RenderType.cutout()));
   }

   public static void transformFlag(Transform<?> flag, PostboxBlockEntity be, float partialTicks) {
      float value = be.flag.getValue(partialTicks);
      float progress = (float)Math.pow((double)Math.min(value * 5.0F, 1.0F), 2.0);
      if (be.flag.getChaseTarget() > 0.0F && !be.flag.settled() && progress == 1.0F) {
         float wiggleProgress = (value - 0.2F) / 0.8F;
         progress = (float)(
            (double)progress + Math.sin((double)(wiggleProgress * (float) (Math.PI * 2) * 4.0F)) / 8.0 / (double)Math.max(1.0F, 8.0F * wiggleProgress)
         );
      }

      flag.translate(0.0F, 0.625F, 0.125F);
      flag.rotateXDegrees(-progress * 90.0F);
      flag.translateBack(0.0F, 0.625F, 0.125F);
   }
}
