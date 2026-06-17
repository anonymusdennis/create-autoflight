package com.simibubi.create.content.kinetics.deployer;

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

public class DeployerFilterSlot extends ValueBoxTransform.Sided {
   @Override
   public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
      Direction facing = (Direction)state.getValue(DeployerBlock.FACING);
      Vec3 vec = VecHelper.voxelSpace(8.0, 8.0, 15.5);
      vec = VecHelper.rotateCentered(vec, (double)AngleHelper.horizontalAngle(this.getSide()), Axis.Y);
      vec = VecHelper.rotateCentered(vec, (double)AngleHelper.verticalAngle(this.getSide()), Axis.X);
      return vec.subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.125));
   }

   @Override
   protected boolean isSideActive(BlockState state, Direction direction) {
      Direction facing = (Direction)state.getValue(DeployerBlock.FACING);
      return direction.getAxis() == facing.getAxis() ? false : ((DeployerBlock)state.getBlock()).getRotationAxis(state) != direction.getAxis();
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      Direction facing = this.getSide();
      float xRot = facing == Direction.UP ? 90.0F : (facing == Direction.DOWN ? 270.0F : 0.0F);
      float yRot = AngleHelper.horizontalAngle(facing) + 180.0F;
      if (facing.getAxis() == Axis.Y) {
         TransformStack.of(ms).rotateYDegrees(180.0F + AngleHelper.horizontalAngle((Direction)state.getValue(DeployerBlock.FACING)));
      }

      ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(yRot)).rotateXDegrees(xRot);
   }

   @Override
   protected Vec3 getSouthLocation() {
      return Vec3.ZERO;
   }
}
