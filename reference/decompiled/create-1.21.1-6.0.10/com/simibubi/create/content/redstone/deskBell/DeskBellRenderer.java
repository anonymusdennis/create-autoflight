package com.simibubi.create.content.redstone.deskBell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class DeskBellRenderer extends SmartBlockEntityRenderer<DeskBellBlockEntity> {
   public DeskBellRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(DeskBellBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = blockEntity.getBlockState();
      float p = blockEntity.animation.getValue(partialTicks);
      if (!((double)p < 0.004) || blockState.getOptionalValue(DeskBellBlock.POWERED).orElse(false)) {
         float f = (float)(1.0 - 4.0 * Math.pow(Math.max((double)p - 0.5, 0.0) - 0.5, 2.0));
         float f2 = (float)Math.pow((double)p, 1.25);
         Direction facing = (Direction)blockState.getValue(DeskBellBlock.FACING);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(
                              AllPartialModels.DESK_BELL_PLUNGER, blockState
                           )
                           .center())
                        .rotateYDegrees(AngleHelper.horizontalAngle(facing)))
                     .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90.0F))
                  .uncenter())
               .translate(0.0F, f * -0.75F / 16.0F, 0.0F))
            .light(light)
            .overlay(overlay)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()));
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(
                                          AllPartialModels.DESK_BELL_BELL, blockState
                                       )
                                       .center())
                                    .rotateYDegrees(AngleHelper.horizontalAngle(facing)))
                                 .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90.0F))
                              .translate(0.0F, 0.0F, 0.0F))
                           .rotateXDegrees(f2 * 8.0F * Mth.sin(p * (float) Math.PI * 4.0F + blockEntity.animationOffset)))
                        .rotateZDegrees(f2 * 8.0F * Mth.cos(p * (float) Math.PI * 4.0F + blockEntity.animationOffset)))
                     .translate(0.0F, 0.0F, 0.0F))
                  .scale(0.995F))
               .uncenter())
            .light(light)
            .overlay(overlay)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }
}
