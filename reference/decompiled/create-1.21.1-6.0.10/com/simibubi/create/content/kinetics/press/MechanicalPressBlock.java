package com.simibubi.create.content.kinetics.press;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MechanicalPressBlock extends HorizontalKineticBlock implements IBE<MechanicalPressBlockEntity> {
   public MechanicalPressBlock(Properties properties) {
      super(properties);
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return context instanceof EntityCollisionContext && ((EntityCollisionContext)context).getEntity() instanceof Player
         ? AllShapes.CASING_14PX.get(Direction.DOWN)
         : AllShapes.MECHANICAL_PROCESSOR_SHAPE;
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      return !BasinBlock.isBasin(worldIn, pos.below());
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction prefferedSide = this.getPreferredHorizontalFacing(context);
      return prefferedSide != null ? (BlockState)this.defaultBlockState().setValue(HORIZONTAL_FACING, prefferedSide) : super.getStateForPlacement(context);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(HORIZONTAL_FACING)).getAxis();
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == ((Direction)state.getValue(HORIZONTAL_FACING)).getAxis();
   }

   @Override
   public Class<MechanicalPressBlockEntity> getBlockEntityClass() {
      return MechanicalPressBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends MechanicalPressBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends MechanicalPressBlockEntity>)AllBlockEntityTypes.MECHANICAL_PRESS.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }
}
