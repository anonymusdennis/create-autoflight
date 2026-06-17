package dev.simulated_team.simulated.content.blocks.util;

import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractDirectionalAxisBlock extends DirectionalBlock implements TransformableBlock, IWrenchable {
   public static final BooleanProperty AXIS_ALONG_FIRST_COORDINATE = BooleanProperty.create("axis_along_first");

   public AbstractDirectionalAxisBlock(Properties properties) {
      super(properties);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      pBuilder.add(new Property[]{AXIS_ALONG_FIRST_COORDINATE, FACING});
      super.createBlockStateDefinition(pBuilder);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      boolean shift = context.isSecondaryUseActive();
      Direction facing = this.getFacingForPlacement(context);
      boolean alongFirst = false;
      Axis faceAxis = facing.getAxis();
      if (faceAxis.isHorizontal()) {
         alongFirst = faceAxis == Axis.Z;
      }

      if (faceAxis.isVertical()) {
         alongFirst = this.getAxisAlignmentForPlacement(context);
      }

      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, facing)).setValue(AXIS_ALONG_FIRST_COORDINATE, shift != alongFirst);
   }

   protected boolean getAxisAlignmentForPlacement(BlockPlaceContext context) {
      return context.getHorizontalDirection().getAxis() == Axis.Z;
   }

   public static Axis getAxis(BlockState state) {
      if (state.getBlock() instanceof AbstractDirectionalAxisBlock) {
         Direction facing = (Direction)state.getValue(FACING);
         Axis gatheredAxis;
         if (facing.getAxis().isVertical()) {
            gatheredAxis = state.getValue(AXIS_ALONG_FIRST_COORDINATE) ? Axis.X : Axis.Z;
         } else {
            boolean facingUp = (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE) != (facing.getStepX() == 0);
            gatheredAxis = facingUp ? Axis.Y : facing.getClockWise().getAxis();
         }

         return gatheredAxis;
      } else {
         return Axis.Y;
      }
   }

   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      return targetedFace == originalState.getValue(FACING)
         ? (BlockState)super.getRotatedBlockState(originalState, targetedFace)
            .setValue(AXIS_ALONG_FIRST_COORDINATE, !(Boolean)originalState.getValue(AXIS_ALONG_FIRST_COORDINATE))
         : super.getRotatedBlockState(originalState, targetedFace);
   }

   @Nullable
   public static Direction getDirectionOfAxis(BlockState state) {
      if (state.getBlock() instanceof AbstractDirectionalAxisBlock) {
         Axis axis = getAxis(state);
         return Direction.get(AxisDirection.POSITIVE, axis);
      } else {
         return null;
      }
   }

   protected Direction getFacingForPlacement(BlockPlaceContext context) {
      return context.getClickedFace();
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      if (rot.ordinal() % 2 == 1) {
         state = (BlockState)state.cycle(AXIS_ALONG_FIRST_COORDINATE);
      }

      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

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
}
