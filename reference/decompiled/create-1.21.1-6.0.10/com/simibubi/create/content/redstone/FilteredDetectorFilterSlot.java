package com.simibubi.create.content.redstone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class FilteredDetectorFilterSlot extends ValueBoxTransform.Sided {
   private boolean hasSlotAtBottom;

   public FilteredDetectorFilterSlot(boolean hasSlotAtBottom) {
      this.hasSlotAtBottom = hasSlotAtBottom;
   }

   @Override
   protected boolean isSideActive(BlockState state, Direction direction) {
      Direction targetDirection = DirectedDirectionalBlock.getTargetDirection(state);
      if (direction == targetDirection) {
         return false;
      } else if (targetDirection.getOpposite() == direction) {
         return true;
      } else if (targetDirection.getAxis() == Axis.Y) {
         if (targetDirection == Direction.UP) {
            direction = direction.getOpposite();
         }

         return !this.hasSlotAtBottom
            ? direction == state.getValue(DirectedDirectionalBlock.FACING)
            : direction.getAxis() == ((Direction)state.getValue(DirectedDirectionalBlock.FACING)).getClockWise().getAxis();
      } else {
         return direction == Direction.UP || direction == Direction.DOWN && this.hasSlotAtBottom;
      }
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      super.rotate(level, pos, state, ms);
      Direction facing = (Direction)state.getValue(DirectedDirectionalBlock.FACING);
      if (facing.getAxis() != Axis.Y) {
         if (this.getSide() == Direction.UP) {
            TransformStack.of(ms).rotateZDegrees(-AngleHelper.horizontalAngle(facing) + 180.0F);
         }
      }
   }

   @Override
   protected Vec3 getSouthLocation() {
      return VecHelper.voxelSpace(8.0, 8.0, 15.5);
   }
}
