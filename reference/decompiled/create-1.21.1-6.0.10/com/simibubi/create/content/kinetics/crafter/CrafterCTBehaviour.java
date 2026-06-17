package com.simibubi.create.content.kinetics.crafter;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CrafterCTBehaviour extends ConnectedTextureBehaviour.Base {
   @Override
   public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
      if (state.getBlock() != other.getBlock()) {
         return false;
      } else {
         return state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING) != other.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)
            ? false
            : CrafterHelper.areCraftersConnected(reader, pos, otherPos);
      }
   }

   @Override
   protected boolean reverseUVs(BlockState state, Direction direction) {
      if (!direction.getAxis().isVertical()) {
         return false;
      } else {
         Direction facing = (Direction)state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
         if (facing.getAxis() == direction.getAxis()) {
            return false;
         } else {
            boolean isNegative = facing.getAxisDirection() == AxisDirection.NEGATIVE;
            return direction == Direction.DOWN && facing.getAxis() == Axis.Z ? !isNegative : isNegative;
         }
      }
   }

   @Override
   public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
      Direction facing = (Direction)state.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
      boolean isFront = facing.getAxis() == direction.getAxis();
      boolean isVertical = direction.getAxis().isVertical();
      boolean facingX = facing.getAxis() == Axis.X;
      return isFront ? AllSpriteShifts.BRASS_CASING : (isVertical && !facingX ? AllSpriteShifts.CRAFTER_OTHERSIDE : AllSpriteShifts.CRAFTER_SIDE);
   }
}
