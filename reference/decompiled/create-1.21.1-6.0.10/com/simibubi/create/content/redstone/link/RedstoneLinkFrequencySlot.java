package com.simibubi.create.content.redstone.link;

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

public class RedstoneLinkFrequencySlot extends ValueBoxTransform.Dual {
   Vec3 horizontal = VecHelper.voxelSpace(10.0, 5.5, 2.5);
   Vec3 vertical = VecHelper.voxelSpace(10.0, 2.5, 5.5);

   public RedstoneLinkFrequencySlot(boolean first) {
      super(first);
   }

   @Override
   public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
      Direction facing = (Direction)state.getValue(RedstoneLinkBlock.FACING);
      Vec3 location = VecHelper.voxelSpace(8.0, 3.01F, 5.5);
      if (facing.getAxis().isHorizontal()) {
         location = VecHelper.voxelSpace(8.0, 5.5, 3.01F);
         if (this.isFirst()) {
            location = location.add(0.0, 0.3125, 0.0);
         }

         return this.rotateHorizontally(state, location);
      } else {
         if (this.isFirst()) {
            location = location.add(0.0, 0.0, 0.3125);
         }

         return VecHelper.rotateCentered(location, facing == Direction.DOWN ? 180.0 : 0.0, Axis.X);
      }
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      Direction facing = (Direction)state.getValue(RedstoneLinkBlock.FACING);
      float yRot = facing.getAxis().isVertical() ? 0.0F : AngleHelper.horizontalAngle(facing) + 180.0F;
      float xRot = facing == Direction.UP ? 90.0F : (facing == Direction.DOWN ? 270.0F : 0.0F);
      ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(yRot)).rotateXDegrees(xRot);
   }

   @Override
   public float getScale() {
      return 0.4975F;
   }
}
