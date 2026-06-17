package com.simibubi.create.content.contraptions.chassis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class StickerRenderer extends SafeBlockEntityRenderer<StickerBlockEntity> {
   public StickerRenderer(Context context) {
   }

   protected void renderSafe(StickerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState state = be.getBlockState();
         SuperByteBuffer head = CachedBuffers.partial(AllPartialModels.STICKER_HEAD, state);
         float offset = be.piston.getValue(AnimationTickHolder.getPartialTicks(be.getLevel()));
         if (be.getLevel() != Minecraft.getInstance().level && !be.isVirtual()) {
            offset = state.getValue(StickerBlock.EXTENDED) ? 1.0F : 0.0F;
         }

         Direction facing = (Direction)state.getValue(StickerBlock.FACING);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)head.nudge(be.hashCode())).center())
                     .rotateYDegrees(AngleHelper.horizontalAngle(facing)))
                  .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90.0F))
               .uncenter())
            .translate(0.0F, offset * offset * 4.0F / 16.0F, 0.0F);
         head.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }
}
