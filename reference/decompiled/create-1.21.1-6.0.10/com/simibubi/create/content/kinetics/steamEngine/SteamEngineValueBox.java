package com.simibubi.create.content.kinetics.steamEngine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.Pointing;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class SteamEngineValueBox extends ValueBoxTransform.Sided {
   @Override
   protected boolean isSideActive(BlockState state, Direction side) {
      Direction engineFacing = SteamEngineBlock.getFacing(state);
      if (engineFacing.getAxis() == side.getAxis()) {
         return false;
      } else {
         float roll = 0.0F;

         for (Pointing p : Pointing.values()) {
            if (p.getCombinedDirection(engineFacing) == side) {
               roll = (float)p.getXRotation();
            }
         }

         if (engineFacing == Direction.UP) {
            roll += 180.0F;
         }

         boolean recessed = roll % 180.0F == 0.0F;
         if (engineFacing.getAxis() == Axis.Y) {
            recessed ^= ((Direction)state.getValue(SteamEngineBlock.FACING)).getAxis() == Axis.X;
         }

         return !recessed;
      }
   }

   @Override
   public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
      Direction side = this.getSide();
      Direction engineFacing = SteamEngineBlock.getFacing(state);
      float roll = 0.0F;

      for (Pointing p : Pointing.values()) {
         if (p.getCombinedDirection(engineFacing) == side) {
            roll = (float)p.getXRotation();
         }
      }

      if (engineFacing == Direction.UP) {
         roll += 180.0F;
      }

      float horizontalAngle = AngleHelper.horizontalAngle(engineFacing);
      float verticalAngle = AngleHelper.verticalAngle(engineFacing);
      Vec3 local = VecHelper.voxelSpace(8.0, 14.5, 9.0);
      local = VecHelper.rotateCentered(local, (double)roll, Axis.Z);
      local = VecHelper.rotateCentered(local, (double)horizontalAngle, Axis.Y);
      return VecHelper.rotateCentered(local, (double)verticalAngle, Axis.X);
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      Direction facing = SteamEngineBlock.getFacing(state);
      if (facing.getAxis() == Axis.Y) {
         super.rotate(level, pos, state, ms);
      } else {
         float roll = 0.0F;

         for (Pointing p : Pointing.values()) {
            if (p.getCombinedDirection(facing) == this.getSide()) {
               roll = (float)p.getXRotation();
            }
         }

         float yRot = AngleHelper.horizontalAngle(facing) + (float)(facing == Direction.DOWN ? 180 : 0);
         ((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(yRot)).rotateXDegrees(facing == Direction.DOWN ? -90.0F : 90.0F))
            .rotateYDegrees(roll);
      }
   }

   @Override
   protected Vec3 getSouthLocation() {
      return Vec3.ZERO;
   }
}
