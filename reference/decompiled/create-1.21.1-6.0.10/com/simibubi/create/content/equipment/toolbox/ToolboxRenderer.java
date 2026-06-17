package com.simibubi.create.content.equipment.toolbox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ToolboxRenderer extends SmartBlockEntityRenderer<ToolboxBlockEntity> {
   public ToolboxRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(ToolboxBlockEntity blockEntity, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = blockEntity.getBlockState();
      Direction facing = ((Direction)blockState.getValue(ToolboxBlock.FACING)).getOpposite();
      SuperByteBuffer lid = CachedBuffers.partial(AllPartialModels.TOOLBOX_LIDS.get(blockEntity.getColor()), blockState);
      SuperByteBuffer drawer = CachedBuffers.partial(AllPartialModels.TOOLBOX_DRAWER, blockState);
      float lidAngle = blockEntity.lid.getValue(partialTicks);
      float drawerOffset = blockEntity.drawers.getValue(partialTicks);
      VertexConsumer builder = buffer.getBuffer(RenderType.cutoutMipped());
      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)lid.center())
                        .rotateYDegrees(-facing.toYRot()))
                     .uncenter())
                  .translate(0.0F, 0.375F, 0.75F))
               .rotateXDegrees(135.0F * lidAngle))
            .translate(0.0F, -0.375F, -0.75F))
         .light(light)
         .renderInto(ms, builder);

      for (int offset : Iterate.zeroAndOne) {
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)drawer.center()).rotateYDegrees(-facing.toYRot())).uncenter())
               .translate(0.0F, (float)(offset * 1) / 8.0F, -drawerOffset * 0.175F * (float)(2 - offset)))
            .light(light)
            .renderInto(ms, builder);
      }
   }
}
