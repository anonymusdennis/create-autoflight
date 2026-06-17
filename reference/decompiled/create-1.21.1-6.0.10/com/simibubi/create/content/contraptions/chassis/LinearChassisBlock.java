package com.simibubi.create.content.contraptions.chassis;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public class LinearChassisBlock extends AbstractChassisBlock {
   public static final BooleanProperty STICKY_TOP = BooleanProperty.create("sticky_top");
   public static final BooleanProperty STICKY_BOTTOM = BooleanProperty.create("sticky_bottom");

   public LinearChassisBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(STICKY_TOP, false)).setValue(STICKY_BOTTOM, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{STICKY_TOP, STICKY_BOTTOM});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockPos placedOnPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
      BlockState blockState = context.getLevel().getBlockState(placedOnPos);
      if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
         return super.getStateForPlacement(context);
      } else {
         return isChassis(blockState)
            ? (BlockState)this.defaultBlockState().setValue(AXIS, (Axis)blockState.getValue(AXIS))
            : (BlockState)this.defaultBlockState().setValue(AXIS, context.getNearestLookingDirection().getAxis());
      }
   }

   public BlockState updateShape(BlockState state, Direction side, BlockState other, LevelAccessor p_196271_4_, BlockPos p_196271_5_, BlockPos p_196271_6_) {
      BooleanProperty property = this.getGlueableSide(state, side);
      return property != null && sameKind(state, other) && state.getValue(AXIS) == other.getValue(AXIS) ? (BlockState)state.setValue(property, false) : state;
   }

   @Override
   public BooleanProperty getGlueableSide(BlockState state, Direction face) {
      if (face.getAxis() != state.getValue(AXIS)) {
         return null;
      } else {
         return face.getAxisDirection() == AxisDirection.POSITIVE ? STICKY_TOP : STICKY_BOTTOM;
      }
   }

   @Override
   protected boolean glueAllowedOnSide(BlockGetter world, BlockPos pos, BlockState state, Direction side) {
      BlockState other = world.getBlockState(pos.relative(side));
      return !sameKind(other, state) || state.getValue(AXIS) != other.getValue(AXIS);
   }

   public static boolean isChassis(BlockState state) {
      return AllBlocks.LINEAR_CHASSIS.has(state) || AllBlocks.SECONDARY_LINEAR_CHASSIS.has(state);
   }

   public static boolean sameKind(BlockState state1, BlockState state2) {
      return state1.getBlock() == state2.getBlock();
   }

   public static class ChassisCTBehaviour extends ConnectedTextureBehaviour.Base {
      @Override
      public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
         Block block = state.getBlock();
         BooleanProperty glueableSide = ((LinearChassisBlock)block).getGlueableSide(state, direction);
         if (glueableSide == null) {
            return AllBlocks.LINEAR_CHASSIS.has(state) ? AllSpriteShifts.CHASSIS_SIDE : AllSpriteShifts.SECONDARY_CHASSIS_SIDE;
         } else {
            return state.getValue(glueableSide) ? AllSpriteShifts.CHASSIS_STICKY : AllSpriteShifts.CHASSIS;
         }
      }

      @Override
      protected Direction getUpDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
         Axis axis = (Axis)state.getValue(RotatedPillarBlock.AXIS);
         return face.getAxis() == axis ? super.getUpDirection(reader, pos, state, face) : Direction.get(AxisDirection.POSITIVE, axis);
      }

      @Override
      protected Direction getRightDirection(BlockAndTintGetter reader, BlockPos pos, BlockState state, Direction face) {
         Axis axis = (Axis)state.getValue(RotatedPillarBlock.AXIS);
         return axis != face.getAxis() && axis.isHorizontal()
            ? (face.getAxis().isHorizontal() ? Direction.DOWN : (axis == Axis.X ? Direction.NORTH : Direction.EAST))
            : super.getRightDirection(reader, pos, state, face);
      }

      @Override
      protected boolean reverseUVsHorizontally(BlockState state, Direction face) {
         Axis axis = (Axis)state.getValue(RotatedPillarBlock.AXIS);
         boolean side = face.getAxis() != axis;
         return side && axis == Axis.X && face.getAxis().isHorizontal() ? true : super.reverseUVsHorizontally(state, face);
      }

      @Override
      protected boolean reverseUVsVertically(BlockState state, Direction face) {
         return super.reverseUVsVertically(state, face);
      }

      @Override
      public boolean reverseUVs(BlockState state, Direction face) {
         Axis axis = (Axis)state.getValue(RotatedPillarBlock.AXIS);
         boolean end = face.getAxis() == axis;
         if (end && axis.isHorizontal() && face.getAxisDirection() == AxisDirection.POSITIVE) {
            return true;
         } else {
            return !end && axis.isHorizontal() && face == Direction.DOWN ? true : super.reverseUVs(state, face);
         }
      }

      @Override
      public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
         Axis axis = (Axis)state.getValue(RotatedPillarBlock.AXIS);
         boolean superConnect = face.getAxis() == axis
            ? super.connectsTo(state, other, reader, pos, otherPos, face)
            : LinearChassisBlock.sameKind(state, other);
         return superConnect && axis == other.getValue(RotatedPillarBlock.AXIS);
      }
   }
}
