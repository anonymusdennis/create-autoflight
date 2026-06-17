package com.simibubi.create.content.logistics.funnel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FunnelFilterSlotPositioning extends ValueBoxTransform.Sided {
   @Override
   public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
      Direction side = this.getSide();
      float horizontalAngle = AngleHelper.horizontalAngle(side);
      Direction funnelFacing = FunnelBlock.getFunnelFacing(state);
      float stateAngle = AngleHelper.horizontalAngle(funnelFacing);
      if (state.getBlock() instanceof BeltFunnelBlock) {
         switch ((BeltFunnelBlock.Shape)state.getValue(BeltFunnelBlock.SHAPE)) {
            case EXTENDED:
               return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0, 15.5, 13.0), (double)stateAngle, Axis.Y);
            case PULLING:
            case PUSHING:
               return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0, 12.0, 8.675F), (double)horizontalAngle, Axis.Y);
            case RETRACTED:
            default:
               return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0, 13.0, 7.5), (double)horizontalAngle, Axis.Y);
         }
      } else if (!funnelFacing.getAxis().isHorizontal()) {
         Vec3 southLocation = VecHelper.voxelSpace(8.0, funnelFacing == Direction.DOWN ? 14.0 : 2.0, 15.5);
         return VecHelper.rotateCentered(southLocation, (double)horizontalAngle, Axis.Y);
      } else {
         return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0, 12.2, 8.55F), (double)horizontalAngle, Axis.Y);
      }
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      Direction facing = FunnelBlock.getFunnelFacing(state);
      if (facing.getAxis().isVertical()) {
         super.rotate(level, pos, state, ms);
      } else {
         boolean isBeltFunnel = state.getBlock() instanceof BeltFunnelBlock;
         if (isBeltFunnel && state.getValue(BeltFunnelBlock.SHAPE) != BeltFunnelBlock.Shape.EXTENDED) {
            BeltFunnelBlock.Shape shape = (BeltFunnelBlock.Shape)state.getValue(BeltFunnelBlock.SHAPE);
            super.rotate(level, pos, state, ms);
            if (shape == BeltFunnelBlock.Shape.PULLING || shape == BeltFunnelBlock.Shape.PUSHING) {
               TransformStack.of(ms).rotateXDegrees(-22.5F);
            }
         } else if (state.getBlock() instanceof FunnelBlock) {
            super.rotate(level, pos, state, ms);
            TransformStack.of(ms).rotateXDegrees(-22.5F);
         } else {
            float yRot = AngleHelper.horizontalAngle(AbstractFunnelBlock.getFunnelFacing(state)) + (float)(facing == Direction.DOWN ? 180 : 0);
            ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(yRot)).rotateXDegrees(facing == Direction.DOWN ? -90.0F : 90.0F);
         }
      }
   }

   @Override
   protected boolean isSideActive(BlockState state, Direction direction) {
      Direction facing = FunnelBlock.getFunnelFacing(state);
      if (facing == null) {
         return false;
      } else if (facing.getAxis().isVertical()) {
         return direction.getAxis().isHorizontal();
      } else {
         return state.getBlock() instanceof BeltFunnelBlock && state.getValue(BeltFunnelBlock.SHAPE) == BeltFunnelBlock.Shape.EXTENDED
            ? direction == Direction.UP
            : direction == facing;
      }
   }

   @Override
   protected Vec3 getSouthLocation() {
      return Vec3.ZERO;
   }
}
