package com.simibubi.create.content.redstone;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DirectedDirectionalBlock extends HorizontalDirectionalBlock implements IWrenchable, TransformableBlock {
   public static final EnumProperty<AttachFace> TARGET = EnumProperty.create("target", AttachFace.class);
   public static final MapCodec<DirectedDirectionalBlock> CODEC = simpleCodec(DirectedDirectionalBlock::new);

   public DirectedDirectionalBlock(Properties pProperties) {
      super(pProperties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(TARGET, AttachFace.WALL));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{TARGET, FACING}));
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction[] var2 = pContext.getNearestLookingDirections();
      int var3 = var2.length;
      byte var4 = 0;
      if (var4 < var3) {
         Direction direction = var2[var4];
         BlockState blockstate;
         if (direction.getAxis() == Axis.Y) {
            blockstate = (BlockState)((BlockState)this.defaultBlockState().setValue(TARGET, direction == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR))
               .setValue(FACING, pContext.getHorizontalDirection());
         } else {
            blockstate = (BlockState)((BlockState)this.defaultBlockState().setValue(TARGET, AttachFace.WALL)).setValue(FACING, direction.getOpposite());
         }

         return blockstate;
      } else {
         return null;
      }
   }

   public static Direction getTargetDirection(BlockState pState) {
      switch ((AttachFace)pState.getValue(TARGET)) {
         case CEILING:
            return Direction.UP;
         case FLOOR:
            return Direction.DOWN;
         default:
            return (Direction)pState.getValue(FACING);
      }
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      if (targetedFace.getAxis() == Axis.Y) {
         return IWrenchable.super.getRotatedBlockState(originalState, targetedFace);
      } else {
         Direction targetDirection = getTargetDirection(originalState);
         Direction newFacing = targetDirection.getClockWise(targetedFace.getAxis());
         if (targetedFace.getAxisDirection() == AxisDirection.NEGATIVE) {
            newFacing = newFacing.getOpposite();
         }

         return newFacing.getAxis() == Axis.Y
            ? (BlockState)originalState.setValue(TARGET, newFacing == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
            : (BlockState)((BlockState)originalState.setValue(TARGET, AttachFace.WALL)).setValue(FACING, newFacing);
      }
   }

   @Override
   public BlockState transform(BlockState state, StructureTransform transform) {
      if (transform.mirror != null) {
         state = this.mirror(state, transform.mirror);
      }

      if (transform.rotationAxis == Axis.Y) {
         return this.rotate(state, transform.rotation);
      } else {
         Direction targetDirection = getTargetDirection(state);
         Direction newFacing = transform.rotateFacing(targetDirection);
         return newFacing.getAxis() == Axis.Y
            ? (BlockState)state.setValue(TARGET, newFacing == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR)
            : (BlockState)((BlockState)state.setValue(TARGET, AttachFace.WALL)).setValue(FACING, newFacing);
      }
   }

   @NotNull
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
