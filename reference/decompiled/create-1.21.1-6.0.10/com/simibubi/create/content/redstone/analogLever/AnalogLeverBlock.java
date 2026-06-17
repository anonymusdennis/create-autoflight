package com.simibubi.create.content.redstone.analogLever;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.mixin.accessor.BlockBehaviourAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class AnalogLeverBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<AnalogLeverBlockEntity> {
   public static final MapCodec<AnalogLeverBlock> CODEC = simpleCodec(AnalogLeverBlock::new);

   public AnalogLeverBlock(Properties p_i48402_1_) {
      super(p_i48402_1_);
   }

   public InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player player, BlockHitResult hit) {
      if (worldIn.isClientSide) {
         addParticles(state, worldIn, pos, 1.0F);
         return InteractionResult.SUCCESS;
      } else {
         return this.onBlockEntityUse(worldIn, pos, be -> {
            boolean sneak = player.isShiftKeyDown();
            be.changeState(sneak);
            float f = 0.25F + (float)(be.state + 5) / 15.0F * 0.5F;
            worldIn.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.2F, f);
            return InteractionResult.SUCCESS;
         });
      }
   }

   public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return this.getBlockEntityOptional(blockAccess, pos).map(al -> al.state).orElse(0);
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getDirectSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return getConnectedDirection(blockState) == side ? this.getSignal(blockState, blockAccess, pos, side) : 0;
   }

   @OnlyIn(Dist.CLIENT)
   public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
      this.withBlockEntityDo(worldIn, pos, be -> {
         if (be.state != 0 && rand.nextFloat() < 0.25F) {
            addParticles(stateIn, worldIn, pos, 0.5F);
         }
      });
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!isMoving && state.getBlock() != newState.getBlock()) {
         this.withBlockEntityDo(worldIn, pos, be -> {
            if (be.state != 0) {
               updateNeighbors(state, worldIn, pos);
            }

            worldIn.removeBlockEntity(pos);
         });
      }
   }

   private static void addParticles(BlockState state, LevelAccessor worldIn, BlockPos pos, float alpha) {
      Direction direction = ((Direction)state.getValue(FACING)).getOpposite();
      Direction direction1 = getConnectedDirection(state).getOpposite();
      double d0 = (double)pos.getX() + 0.5 + 0.1 * (double)direction.getStepX() + 0.2 * (double)direction1.getStepX();
      double d1 = (double)pos.getY() + 0.5 + 0.1 * (double)direction.getStepY() + 0.2 * (double)direction1.getStepY();
      double d2 = (double)pos.getZ() + 0.5 + 0.1 * (double)direction.getStepZ() + 0.2 * (double)direction1.getStepZ();
      worldIn.addParticle(new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), alpha), d0, d1, d2, 0.0, 0.0, 0.0);
   }

   static void updateNeighbors(BlockState state, Level world, BlockPos pos) {
      world.updateNeighborsAt(pos, state.getBlock());
      world.updateNeighborsAt(pos.relative(getConnectedDirection(state).getOpposite()), state.getBlock());
   }

   @NotNull
   public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
      return ((BlockBehaviourAccessor)Blocks.LEVER).create$getShape(state, worldIn, pos, context);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{FACING, FACE}));
   }

   @Override
   public Class<AnalogLeverBlockEntity> getBlockEntityClass() {
      return AnalogLeverBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends AnalogLeverBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends AnalogLeverBlockEntity>)AllBlockEntityTypes.ANALOG_LEVER.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @NotNull
   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
