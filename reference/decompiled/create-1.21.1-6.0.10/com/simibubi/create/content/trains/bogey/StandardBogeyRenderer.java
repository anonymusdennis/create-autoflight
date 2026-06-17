package com.simibubi.create.content.trains.bogey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class StandardBogeyRenderer implements BogeyRenderer {
   @Override
   public void render(
      CompoundTag bogeyData,
      float wheelAngle,
      float partialTick,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      int light,
      int overlay,
      boolean inContraption
   ) {
      VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
      SuperByteBuffer shaft = CachedBuffers.block((BlockState)AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Axis.Z));

      for (int i : Iterate.zeroAndOne) {
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)shaft.translate(-0.5F, 0.25F, (float)(i * -1))).center())
                  .rotateZDegrees(wheelAngle))
               .uncenter())
            .light(light)
            .overlay(overlay)
            .renderInto(poseStack, buffer);
      }
   }

   public static class Large extends StandardBogeyRenderer {
      public static final float BELT_RADIUS_PX = 5.0F;
      public static final float BELT_RADIUS_IN_UV_SPACE = 0.3125F;

      @Override
      public void render(
         CompoundTag bogeyData,
         float wheelAngle,
         float partialTick,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int light,
         int overlay,
         boolean inContraption
      ) {
         super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);
         VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
         SuperByteBuffer secondaryShaft = CachedBuffers.block((BlockState)AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Axis.X));

         for (int i : Iterate.zeroAndOne) {
            ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)secondaryShaft.translate(-0.5F, 0.25F, 0.5F + (float)(i * -2))).center())
                     .rotateXDegrees(wheelAngle))
                  .uncenter())
               .light(light)
               .overlay(overlay)
               .renderInto(poseStack, buffer);
         }

         ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.BOGEY_DRIVE, Blocks.AIR.defaultBlockState()).scale(0.9980469F))
            .light(light)
            .overlay(overlay)
            .renderInto(poseStack, buffer);
         float spriteSize = AllSpriteShifts.BOGEY_BELT.getTarget().getV1() - AllSpriteShifts.BOGEY_BELT.getTarget().getV0();
         float scroll = 0.0054541538F * wheelAngle;
         scroll -= (float)Mth.floor(scroll);
         scroll = scroll * spriteSize * 0.5F;
         ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.BOGEY_DRIVE_BELT, Blocks.AIR.defaultBlockState()).scale(0.9980469F))
            .light(light)
            .overlay(overlay)
            .shiftUVScrolling(AllSpriteShifts.BOGEY_BELT, scroll)
            .renderInto(poseStack, buffer);
         ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.BOGEY_PISTON, Blocks.AIR.defaultBlockState())
               .translate(0.0, 0.0, 0.25 * Math.sin((double)AngleHelper.rad((double)wheelAngle))))
            .light(light)
            .overlay(overlay)
            .renderInto(poseStack, buffer);
         ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.LARGE_BOGEY_WHEELS, Blocks.AIR.defaultBlockState())
                  .translate(0.0F, 1.0F, 0.0F))
               .rotateXDegrees(wheelAngle))
            .light(light)
            .overlay(overlay)
            .renderInto(poseStack, buffer);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(
                           AllPartialModels.BOGEY_PIN, Blocks.AIR.defaultBlockState()
                        )
                        .translate(0.0F, 1.0F, 0.0F))
                     .rotateXDegrees(wheelAngle))
                  .translate(0.0F, 0.25F, 0.0F))
               .rotateXDegrees(-wheelAngle))
            .light(light)
            .overlay(overlay)
            .renderInto(poseStack, buffer);
      }
   }

   public static class Small extends StandardBogeyRenderer {
      @Override
      public void render(
         CompoundTag bogeyData,
         float wheelAngle,
         float partialTick,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int light,
         int overlay,
         boolean inContraption
      ) {
         super.render(bogeyData, wheelAngle, partialTick, poseStack, bufferSource, light, overlay, inContraption);
         VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutoutMipped());
         ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.BOGEY_FRAME, Blocks.AIR.defaultBlockState()).scale(0.9980469F))
            .light(light)
            .overlay(overlay)
            .renderInto(poseStack, buffer);
         SuperByteBuffer wheels = CachedBuffers.partial(AllPartialModels.SMALL_BOGEY_WHEELS, Blocks.AIR.defaultBlockState());

         for (int side : Iterate.positiveAndNegative) {
            ((SuperByteBuffer)((SuperByteBuffer)wheels.translate(0.0F, 0.75F, (float)side)).rotateXDegrees(wheelAngle))
               .light(light)
               .overlay(overlay)
               .renderInto(poseStack, buffer);
         }
      }
   }
}
