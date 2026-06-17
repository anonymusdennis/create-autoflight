package com.simibubi.create.content.trains.bogey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class BogeyBlockEntityRenderer<T extends AbstractBogeyBlockEntity> extends SafeBlockEntityRenderer<T> {
   public BogeyBlockEntityRenderer(Context context) {
   }

   protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = be.getBlockState();
      if (blockState.getBlock() instanceof AbstractBogeyBlock<?> bogey) {
         float var10 = be.getVirtualAngle(partialTicks);
         ms.pushPose();
         ms.translate(0.5F, 0.5F, 0.5F);
         if (blockState.getValue(AbstractBogeyBlock.AXIS) == Axis.X) {
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
         }

         be.getStyle().render(bogey.getSize(), partialTicks, ms, buffer, light, overlay, var10, be.getBogeyData(), false);
         ms.popPose();
      }
   }
}
