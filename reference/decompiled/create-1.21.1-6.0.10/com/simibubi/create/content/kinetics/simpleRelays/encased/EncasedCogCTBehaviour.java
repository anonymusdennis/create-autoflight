package com.simibubi.create.content.kinetics.simpleRelays.encased;

import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import net.createmod.catnip.data.Couple;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class EncasedCogCTBehaviour extends EncasedCTBehaviour {
   private Couple<CTSpriteShiftEntry> sideShifts;
   private boolean large;

   public EncasedCogCTBehaviour(CTSpriteShiftEntry shift) {
      this(shift, null);
   }

   public EncasedCogCTBehaviour(CTSpriteShiftEntry shift, Couple<CTSpriteShiftEntry> sideShifts) {
      super(shift);
      this.large = sideShifts == null;
      this.sideShifts = sideShifts;
   }

   @Override
   public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
      Axis axis = (Axis)state.getValue(RotatedPillarKineticBlock.AXIS);
      if (!this.large && axis != face.getAxis()) {
         if (other.getBlock() == state.getBlock() && other.getValue(RotatedPillarKineticBlock.AXIS) == state.getValue(RotatedPillarKineticBlock.AXIS)) {
            return true;
         } else {
            BlockState blockState = reader.getBlockState(otherPos.relative(face));
            return !ICogWheel.isLargeCog(blockState) ? false : ((IRotate)blockState.getBlock()).getRotationAxis(blockState) == axis;
         }
      } else {
         return super.connectsTo(state, other, reader, pos, otherPos, face);
      }
   }

   @Override
   protected boolean reverseUVs(BlockState state, Direction face) {
      return ((Axis)state.getValue(RotatedPillarKineticBlock.AXIS)).isHorizontal()
         && face.getAxis().isHorizontal()
         && face.getAxisDirection() == AxisDirection.POSITIVE;
   }

   @Override
   protected boolean reverseUVsVertically(BlockState state, Direction face) {
      return !this.large && state.getValue(RotatedPillarKineticBlock.AXIS) == Axis.X && face.getAxis() == Axis.Z
         ? face != Direction.SOUTH
         : super.reverseUVsVertically(state, face);
   }

   @Override
   protected boolean reverseUVsHorizontally(BlockState state, Direction face) {
      if (this.large) {
         return super.reverseUVsHorizontally(state, face);
      } else if (((Axis)state.getValue(RotatedPillarKineticBlock.AXIS)).isVertical() && face.getAxis().isHorizontal()) {
         return true;
      } else {
         return state.getValue(RotatedPillarKineticBlock.AXIS) == Axis.Z && face == Direction.DOWN ? true : super.reverseUVsHorizontally(state, face);
      }
   }

   @Override
   public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
      Axis axis = (Axis)state.getValue(RotatedPillarKineticBlock.AXIS);
      if (!this.large && axis != direction.getAxis()) {
         return (CTSpriteShiftEntry)this.sideShifts.get(axis == Axis.X || axis == Axis.Z && direction.getAxis() == Axis.X);
      } else {
         return axis == direction.getAxis()
               && state.getValue(direction.getAxisDirection() == AxisDirection.POSITIVE ? EncasedCogwheelBlock.TOP_SHAFT : EncasedCogwheelBlock.BOTTOM_SHAFT)
            ? null
            : super.getShift(state, direction, sprite);
      }
   }
}
