package com.simibubi.create.content.redstone.displayLink;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class LinkBulbRenderer extends SafeBlockEntityRenderer<LinkWithBulbBlockEntity> {
   public LinkBulbRenderer(Context context) {
   }

   protected void renderSafe(LinkWithBulbBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      float glow = be.getGlow(partialTicks);
      if (!(glow < 0.125F)) {
         glow = (float)(1.0 - 2.0 * Math.pow((double)(glow - 0.75F), 2.0));
         glow = Mth.clamp(glow, -1.0F, 1.0F);
         int color = (int)(200.0F * glow);
         BlockState blockState = be.getBlockState();
         PoseTransformStack msr = TransformStack.of(ms);
         Direction face = be.getBulbFacing(blockState);
         ms.pushPose();
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)msr.center()).rotateYDegrees(AngleHelper.horizontalAngle(face) + 180.0F))
               .rotateXDegrees(-AngleHelper.verticalAngle(face) - 90.0F))
            .uncenter();
         ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.DISPLAY_LINK_TUBE, blockState).translate(be.getBulbOffset(blockState)))
            .light(15728880)
            .renderInto(ms, buffer.getBuffer(RenderType.translucent()));
         ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.DISPLAY_LINK_GLOW, blockState).translate(be.getBulbOffset(blockState)))
            .light(15728880)
            .color(color, color, color, 255)
            .disableDiffuse()
            .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
         ms.popPose();
      }
   }
}
