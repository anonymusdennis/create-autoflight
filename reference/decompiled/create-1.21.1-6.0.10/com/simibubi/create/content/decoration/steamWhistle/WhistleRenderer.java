package com.simibubi.create.content.decoration.steamWhistle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class WhistleRenderer extends SafeBlockEntityRenderer<WhistleBlockEntity> {
   public WhistleRenderer(Context context) {
   }

   protected void renderSafe(WhistleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = be.getBlockState();
      if (blockState.getBlock() instanceof WhistleBlock) {
         Direction direction = (Direction)blockState.getValue(WhistleBlock.FACING);
         WhistleBlock.WhistleSize size = (WhistleBlock.WhistleSize)blockState.getValue(WhistleBlock.SIZE);
         PartialModel mouth = size == WhistleBlock.WhistleSize.LARGE
            ? AllPartialModels.WHISTLE_MOUTH_LARGE
            : (size == WhistleBlock.WhistleSize.MEDIUM ? AllPartialModels.WHISTLE_MOUTH_MEDIUM : AllPartialModels.WHISTLE_MOUTH_SMALL);
         float offset = be.animation.getValue(partialTicks);
         if (be.animation.getChaseTarget() > 0.0F && be.animation.getValue() > 0.5F) {
            float wiggleProgress = ((float)AnimationTickHolder.getTicks(be.getLevel()) + partialTicks) / 8.0F;
            offset = (float)((double)offset - Math.sin((double)(wiggleProgress * (float) (Math.PI * 2) * (float)(4 - size.ordinal()))) / 16.0);
         }

         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(mouth, blockState).center())
                     .rotateYDegrees(AngleHelper.horizontalAngle(direction)))
                  .uncenter())
               .translate(0.0F, offset * 4.0F / 16.0F, 0.0F))
            .light(light)
            .renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }
}
