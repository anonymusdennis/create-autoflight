package com.simibubi.create.content.kinetics.flywheel;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlywheelBlock extends RotatedPillarKineticBlock implements IBE<FlywheelBlockEntity> {
   public FlywheelBlock(Properties properties) {
      super(properties);
   }

   @Override
   public Class<FlywheelBlockEntity> getBlockEntityClass() {
      return FlywheelBlockEntity.class;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.LARGE_GEAR.get((Axis)pState.getValue(AXIS));
   }

   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Override
   public BlockEntityType<? extends FlywheelBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends FlywheelBlockEntity>)AllBlockEntityTypes.FLYWHEEL.get();
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == this.getRotationAxis(state);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(AXIS);
   }

   @Override
   public float getParticleTargetRadius() {
      return 2.0F;
   }

   @Override
   public float getParticleInitialRadius() {
      return 1.75F;
   }
}
