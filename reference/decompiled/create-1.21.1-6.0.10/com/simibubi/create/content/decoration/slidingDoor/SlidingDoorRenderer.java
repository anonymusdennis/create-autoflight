package com.simibubi.create.content.decoration.slidingDoor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public class SlidingDoorRenderer extends SafeBlockEntityRenderer<SlidingDoorBlockEntity> {
   public SlidingDoorRenderer(Context context) {
   }

   protected void renderSafe(SlidingDoorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = be.getBlockState();
      if (be.shouldRenderSpecial(blockState)) {
         Direction facing = (Direction)blockState.getValue(DoorBlock.FACING);
         Direction movementDirection = facing.getClockWise();
         if (blockState.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT) {
            movementDirection = movementDirection.getOpposite();
         }

         float value = be.animation.getValue(partialTicks);
         float value2 = Mth.clamp(value * 10.0F, 0.0F, 1.0F);
         VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
         Vec3 offset = Vec3.atLowerCornerOf(movementDirection.getNormal())
            .scale((double)(value * value * 13.0F / 16.0F))
            .add(Vec3.atLowerCornerOf(facing.getNormal()).scale((double)(value2 * 1.0F / 32.0F)));
         if (((SlidingDoorBlock)blockState.getBlock()).isFoldingDoor()) {
            Couple<PartialModel> partials = AllPartialModels.FOLDING_DOORS.get(BuiltInRegistries.BLOCK.getKey(blockState.getBlock()));
            boolean flip = blockState.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT;

            for (boolean left : Iterate.trueAndFalse) {
               SuperByteBuffer partial = CachedBuffers.partial((PartialModel)partials.get(left ^ flip), blockState);
               float f = flip ? -1.0F : 1.0F;
               ((SuperByteBuffer)partial.translate(0.0F, -0.001953125F, 0.0F))
                  .translate(Vec3.atLowerCornerOf(facing.getNormal()).scale((double)(value2 * 1.0F / 32.0F)));
               partial.rotateCentered((float) (Math.PI / 180.0) * AngleHelper.horizontalAngle(facing.getClockWise()), Direction.UP);
               if (flip) {
                  partial.translate(0.0F, 0.0F, 1.0F);
               }

               partial.rotateYDegrees(91.0F * f * value * value);
               if (!left) {
                  ((SuperByteBuffer)partial.translate(0.0F, 0.0F, f / 2.0F)).rotateYDegrees(-181.0F * f * value * value);
               }

               if (flip) {
                  partial.translate(0.0F, 0.0F, -0.5F);
               }

               partial.light(light).renderInto(ms, vb);
            }
         } else {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
               ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.block(
                           (BlockState)((BlockState)blockState.setValue(DoorBlock.OPEN, false)).setValue(DoorBlock.HALF, half)
                        )
                        .translate(0.0F, half == DoubleBlockHalf.UPPER ? 0.9980469F : 0.0F, 0.0F))
                     .translate(offset))
                  .light(light)
                  .renderInto(ms, vb);
            }
         }
      }
   }
}
