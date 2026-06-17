package com.simibubi.create.content.logistics.itemHatch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class HatchFilterSlot extends ValueBoxTransform {
   @Override
   public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
      return VecHelper.rotateCentered(VecHelper.voxelSpace(8.0, 5.15, 9.5), (double)this.angle(state), Axis.Y);
   }

   @Override
   public float getScale() {
      return super.getScale() * 0.965F;
   }

   @Override
   public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
      return localHit.distanceTo(this.getLocalOffset(level, pos, state).subtract(0.0, 0.125, 0.0)) < (double)(this.scale / 2.0F);
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.angle(state)));
      ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-45.0F));
   }

   private float angle(BlockState state) {
      return AngleHelper.horizontalAngle(state.getOptionalValue(ItemHatchBlock.FACING).orElse(Direction.NORTH));
   }
}
