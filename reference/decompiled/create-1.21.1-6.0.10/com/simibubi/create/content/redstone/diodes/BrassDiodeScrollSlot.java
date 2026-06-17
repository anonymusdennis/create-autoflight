package com.simibubi.create.content.redstone.diodes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class BrassDiodeScrollSlot extends ValueBoxTransform {
   @Override
   public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
      return VecHelper.voxelSpace(8.0, 2.6F, 8.0);
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      float yRot = AngleHelper.horizontalAngle((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING)) + 180.0F;
      ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(yRot)).rotateXDegrees(90.0F);
   }

   @Override
   public int getOverrideColor() {
      return 5841956;
   }
}
