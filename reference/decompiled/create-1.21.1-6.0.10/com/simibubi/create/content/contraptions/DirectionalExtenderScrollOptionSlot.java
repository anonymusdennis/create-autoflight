package com.simibubi.create.content.contraptions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.function.BiPredicate;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class DirectionalExtenderScrollOptionSlot extends CenteredSideValueBoxTransform {
   public DirectionalExtenderScrollOptionSlot(BiPredicate<BlockState, Direction> allowedDirections) {
      super(allowedDirections);
   }

   @Override
   public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
      return super.getLocalOffset(level, pos, state)
         .add(Vec3.atLowerCornerOf(((Direction)state.getValue(BlockStateProperties.FACING)).getNormal()).scale(-0.125));
   }

   @Override
   public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
      if (!this.getSide().getAxis().isHorizontal()) {
         TransformStack.of(ms).rotateYDegrees(AngleHelper.horizontalAngle((Direction)state.getValue(BlockStateProperties.FACING)) + 180.0F);
      }

      super.rotate(level, pos, state, ms);
   }
}
