package com.simibubi.create.content.contraptions.actors.trainControls;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.ContraptionWorld;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class ControlsBlock extends HorizontalDirectionalBlock implements IWrenchable, ProperWaterloggedBlock {
   public static final BooleanProperty OPEN = BooleanProperty.create("open");
   public static final BooleanProperty VIRTUAL = BooleanProperty.create("virtual");
   public static final MapCodec<ControlsBlock> CODEC = simpleCodec(ControlsBlock::new);

   public ControlsBlock(Properties p_54120_) {
      super(p_54120_);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(OPEN, false)).setValue(WATERLOGGED, false)).setValue(VIRTUAL, false)
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{FACING, OPEN, WATERLOGGED, VIRTUAL}));
   }

   public BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return (BlockState)pState.setValue(OPEN, pLevel instanceof ContraptionWorld);
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState state = this.withWater(super.getStateForPlacement(pContext), pContext);
      Direction horizontalDirection = pContext.getHorizontalDirection();
      Player player = pContext.getPlayer();
      state = (BlockState)state.setValue(FACING, horizontalDirection.getOpposite());
      if (player != null && player.isShiftKeyDown()) {
         state = (BlockState)state.setValue(FACING, horizontalDirection);
      }

      return state;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.CONTROLS.get((Direction)pState.getValue(FACING));
   }

   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.CONTROLS_COLLISION.get((Direction)pState.getValue(FACING));
   }

   @NotNull
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
