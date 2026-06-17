package com.simibubi.create.foundation.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

public class WrenchableDirectionalBlock extends DirectionalBlock implements IWrenchable {
   public static final MapCodec<WrenchableDirectionalBlock> CODEC = simpleCodec(WrenchableDirectionalBlock::new);

   public WrenchableDirectionalBlock(Properties properties) {
      super(properties);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
      super.createBlockStateDefinition(builder);
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      Direction facing = (Direction)originalState.getValue(FACING);
      if (facing.getAxis() == targetedFace.getAxis()) {
         return originalState;
      } else {
         Direction newFacing = facing.getClockWise(targetedFace.getAxis());
         return (BlockState)originalState.setValue(FACING, newFacing);
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   @NotNull
   protected MapCodec<? extends DirectionalBlock> codec() {
      return CODEC;
   }
}
