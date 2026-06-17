package com.simibubi.create.content.kinetics.mixer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MechanicalMixerBlock extends KineticBlock implements IBE<MechanicalMixerBlockEntity>, ICogWheel {
   public MechanicalMixerBlock(Properties properties) {
      super(properties);
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      return !BasinBlock.isBasin(worldIn, pos.below());
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return context instanceof EntityCollisionContext && ((EntityCollisionContext)context).getEntity() instanceof Player
         ? AllShapes.CASING_14PX.get(Direction.DOWN)
         : AllShapes.MECHANICAL_PROCESSOR_SHAPE;
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return Axis.Y;
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return false;
   }

   @Override
   public float getParticleTargetRadius() {
      return 0.85F;
   }

   @Override
   public float getParticleInitialRadius() {
      return 0.75F;
   }

   @Override
   public IRotate.SpeedLevel getMinimumRequiredSpeedLevel() {
      return IRotate.SpeedLevel.MEDIUM;
   }

   @Override
   public Class<MechanicalMixerBlockEntity> getBlockEntityClass() {
      return MechanicalMixerBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends MechanicalMixerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends MechanicalMixerBlockEntity>)AllBlockEntityTypes.MECHANICAL_MIXER.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }
}
