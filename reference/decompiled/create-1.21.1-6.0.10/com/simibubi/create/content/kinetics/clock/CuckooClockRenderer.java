package com.simibubi.create.content.kinetics.clock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class CuckooClockRenderer extends KineticBlockEntityRenderer<CuckooClockBlockEntity> {
   public CuckooClockRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(CuckooClockBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      if (be instanceof CuckooClockBlockEntity) {
         BlockState blockState = be.getBlockState();
         Direction direction = (Direction)blockState.getValue(CuckooClockBlock.HORIZONTAL_FACING);
         VertexConsumer vb = buffer.getBuffer(RenderType.solid());
         SuperByteBuffer hourHand = CachedBuffers.partial(AllPartialModels.CUCKOO_HOUR_HAND, blockState);
         SuperByteBuffer minuteHand = CachedBuffers.partial(AllPartialModels.CUCKOO_MINUTE_HAND, blockState);
         float hourAngle = be.hourHand.getValue(partialTicks);
         float minuteAngle = be.minuteHand.getValue(partialTicks);
         this.rotateHand(hourHand, hourAngle, direction).light(light).renderInto(ms, vb);
         this.rotateHand(minuteHand, minuteAngle, direction).light(light).renderInto(ms, vb);
         SuperByteBuffer leftDoor = CachedBuffers.partial(AllPartialModels.CUCKOO_LEFT_DOOR, blockState);
         SuperByteBuffer rightDoor = CachedBuffers.partial(AllPartialModels.CUCKOO_RIGHT_DOOR, blockState);
         float angle = 0.0F;
         float offset = 0.0F;
         if (be.animationType != null) {
            float value = be.animationProgress.getValue(partialTicks);
            int step = be.animationType == CuckooClockBlockEntity.Animation.SURPRISE ? 3 : 15;

            for (int phase = 30; phase <= 60; phase += step) {
               float local = value - (float)phase;
               if (!(local < (float)(-step / 3))) {
                  if (local < 0.0F) {
                     angle = Mth.lerp((value - (float)(phase - 5)) / 5.0F, 0.0F, 135.0F);
                  } else if (local < (float)(step / 3)) {
                     angle = 135.0F;
                  } else if (local < (float)(2 * step / 3)) {
                     angle = Mth.lerp((value - (float)(phase + 5)) / 5.0F, 135.0F, 0.0F);
                  }
               }
            }
         }

         this.rotateDoor(leftDoor, angle, true, direction).light(light).renderInto(ms, vb);
         this.rotateDoor(rightDoor, angle, false, direction).light(light).renderInto(ms, vb);
         if (be.animationType != CuckooClockBlockEntity.Animation.NONE) {
            offset = -(angle / 135.0F) * 1.0F / 2.0F + 0.625F;
            PartialModel partialModel = be.animationType == CuckooClockBlockEntity.Animation.PIG
               ? AllPartialModels.CUCKOO_PIG
               : AllPartialModels.CUCKOO_CREEPER;
            SuperByteBuffer figure = CachedBuffers.partial(partialModel, blockState);
            figure.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(direction.getCounterClockWise())), Direction.UP);
            figure.translate(offset, 0.0F, 0.0F);
            figure.light(light).renderInto(ms, vb);
         }
      }
   }

   protected SuperByteBuffer getRotatedModel(CuckooClockBlockEntity be, BlockState state) {
      return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, ((Direction)state.getValue(CuckooClockBlock.HORIZONTAL_FACING)).getOpposite());
   }

   private SuperByteBuffer rotateHand(SuperByteBuffer buffer, float angle, Direction facing) {
      float pivotX = 0.125F;
      float pivotY = 0.375F;
      float pivotZ = 0.5F;
      buffer.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getCounterClockWise())), Direction.UP);
      buffer.translate(pivotX, pivotY, pivotZ);
      buffer.rotate(AngleHelper.rad((double)angle), Direction.EAST);
      buffer.translate(-pivotX, -pivotY, -pivotZ);
      return buffer;
   }

   private SuperByteBuffer rotateDoor(SuperByteBuffer buffer, float angle, boolean left, Direction facing) {
      float pivotX = 0.125F;
      float pivotY = 0.0F;
      float pivotZ = (float)(left ? 6 : 10) / 16.0F;
      buffer.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getCounterClockWise())), Direction.UP);
      buffer.translate(pivotX, pivotY, pivotZ);
      buffer.rotate(AngleHelper.rad((double)angle) * (float)(left ? -1 : 1), Direction.UP);
      buffer.translate(-pivotX, -pivotY, -pivotZ);
      return buffer;
   }
}
