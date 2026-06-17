package com.simibubi.create.content.equipment.bell;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;

public class BellRenderer<BE extends AbstractBellBlockEntity> extends SafeBlockEntityRenderer<BE> {
   public BellRenderer(Context context) {
   }

   protected void renderSafe(BE be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState state = be.getBlockState();
      Direction facing = (Direction)state.getValue(BellBlock.FACING);
      BellAttachType attachment = (BellAttachType)state.getValue(BellBlock.ATTACHMENT);
      SuperByteBuffer bell = CachedBuffers.partial(be.getBellModel(), state);
      if (be.isRinging) {
         bell.rotateCentered(getSwingAngle((float)be.ringingTicks + partialTicks), be.ringDirection.getCounterClockWise());
      }

      float rY = AngleHelper.horizontalAngle(facing);
      if (attachment == BellAttachType.SINGLE_WALL || attachment == BellAttachType.DOUBLE_WALL) {
         rY += 90.0F;
      }

      bell.rotateCentered(AngleHelper.rad((double)rY), Direction.UP);
      bell.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
   }

   public static float getSwingAngle(float time) {
      float t = time / 1.5F;
      return 1.2F * Mth.sin(t / (float) Math.PI) / (2.5F + t / 3.0F);
   }
}
