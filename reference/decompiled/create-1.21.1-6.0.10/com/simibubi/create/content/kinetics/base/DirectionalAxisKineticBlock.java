package com.simibubi.create.content.kinetics.base;

import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class DirectionalAxisKineticBlock extends DirectionalKineticBlock implements TransformableBlock {
   public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE = BooleanProperty.create("axis_along_first");

   public DirectionalAxisKineticBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{AXIS_ALONG_FIRST_COORDINATE});
      super.createBlockStateDefinition(builder);
   }

   protected Direction getFacingForPlacement(BlockPlaceContext context) {
      Direction facing = context.getNearestLookingDirection().getOpposite();
      if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
         facing = facing.getOpposite();
      }

      return facing;
   }

   protected boolean getAxisAlignmentForPlacement(BlockPlaceContext context) {
      return context.getHorizontalDirection().getAxis() == Axis.X;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing = this.getFacingForPlacement(context);
      BlockPos pos = context.getClickedPos();
      Level world = context.getLevel();
      boolean alongFirst = false;
      Axis faceAxis = facing.getAxis();
      if (faceAxis.isHorizontal()) {
         alongFirst = faceAxis == Axis.Z;
         Direction positivePerpendicular = faceAxis == Axis.X ? Direction.SOUTH : Direction.EAST;
         boolean shaftAbove = this.prefersConnectionTo(world, pos, Direction.UP, true);
         boolean shaftBelow = this.prefersConnectionTo(world, pos, Direction.DOWN, true);
         boolean preferLeft = this.prefersConnectionTo(world, pos, positivePerpendicular, false);
         boolean preferRight = this.prefersConnectionTo(world, pos, positivePerpendicular.getOpposite(), false);
         if (shaftAbove || shaftBelow || preferLeft || preferRight) {
            alongFirst = faceAxis == Axis.X;
         }
      }

      if (faceAxis.isVertical()) {
         alongFirst = this.getAxisAlignmentForPlacement(context);
         Direction prefferedSide = null;

         for (Direction side : Iterate.horizontalDirections) {
            if (this.prefersConnectionTo(world, pos, side, true) || this.prefersConnectionTo(world, pos, side.getClockWise(), false)) {
               if (prefferedSide != null && prefferedSide.getAxis() != side.getAxis()) {
                  prefferedSide = null;
                  break;
               }

               prefferedSide = side;
            }
         }

         if (prefferedSide != null) {
            alongFirst = prefferedSide.getAxis() == Axis.X;
         }
      }

      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(AXIS_ALONG_FIRST_COORDINATE, alongFirst);
   }

   protected boolean prefersConnectionTo(LevelReader reader, BlockPos pos, Direction facing, boolean shaftAxis) {
      if (!shaftAxis) {
         return false;
      } else {
         BlockPos neighbourPos = pos.relative(facing);
         BlockState blockState = reader.getBlockState(neighbourPos);
         Block block = blockState.getBlock();
         return block instanceof IRotate && ((IRotate)block).hasShaftTowards(reader, neighbourPos, blockState, facing.getOpposite());
      }
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      Axis pistonAxis = ((Direction)state.getValue(FACING)).getAxis();
      boolean alongFirst = (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE);
      if (pistonAxis == Axis.X) {
         return alongFirst ? Axis.Y : Axis.Z;
      } else if (pistonAxis == Axis.Y) {
         return alongFirst ? Axis.X : Axis.Z;
      } else if (pistonAxis == Axis.Z) {
         return alongFirst ? Axis.X : Axis.Y;
      } else {
         throw new IllegalStateException("Unknown axis??");
      }
   }

   @Override
   public BlockState rotate(BlockState state, Rotation rot) {
      if (rot.ordinal() % 2 == 1) {
         state = (BlockState)state.cycle(AXIS_ALONG_FIRST_COORDINATE);
      }

      return super.rotate(state, rot);
   }

   @Override
   public BlockState transform(BlockState state, StructureTransform transform) {
      if (transform.mirror != null) {
         state = this.mirror(state, transform.mirror);
      }

      if (transform.rotationAxis == Axis.Y) {
         return this.rotate(state, transform.rotation);
      } else {
         Direction newFacing = transform.rotateFacing((Direction)state.getValue(FACING));
         if (transform.rotationAxis == newFacing.getAxis() && transform.rotation.ordinal() % 2 == 1) {
            state = (BlockState)state.cycle(AXIS_ALONG_FIRST_COORDINATE);
         }

         return (BlockState)state.setValue(FACING, newFacing);
      }
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == this.getRotationAxis(state);
   }
}
