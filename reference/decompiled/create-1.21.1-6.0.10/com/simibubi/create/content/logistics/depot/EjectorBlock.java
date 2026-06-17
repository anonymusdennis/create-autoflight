package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.item.ItemHelper;
import java.util.Optional;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EjectorBlock extends HorizontalKineticBlock implements IBE<EjectorBlockEntity>, ProperWaterloggedBlock {
   public EjectorBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{WATERLOGGED}));
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return pState;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.withWater(super.getStateForPlacement(pContext), pContext);
   }

   public VoxelShape getShape(BlockState p_220053_1_, BlockGetter p_220053_2_, BlockPos p_220053_3_, CollisionContext p_220053_4_) {
      return AllShapes.CASING_13PX.get(Direction.UP);
   }

   public float getFriction(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
      return this.getBlockEntityOptional(world, pos)
         .filter(ete -> ete.state == EjectorBlockEntity.State.LAUNCHING)
         .map($ -> 1.0F)
         .orElse(super.getFriction(state, world, pos, entity));
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
      this.withBlockEntityDo(world, pos, EjectorBlockEntity::updateSignal);
   }

   public void fallOn(Level p_180658_1_, BlockState p_152427_, BlockPos p_180658_2_, Entity p_180658_3_, float p_180658_4_) {
      Optional<EjectorBlockEntity> blockEntityOptional = this.getBlockEntityOptional(p_180658_1_, p_180658_2_);
      if (blockEntityOptional.isPresent() && !p_180658_3_.isSuppressingBounce()) {
         p_180658_3_.causeFallDamage(p_180658_4_, 1.0F, p_180658_1_.damageSources().fall());
      } else {
         super.fallOn(p_180658_1_, p_152427_, p_180658_2_, p_180658_3_, p_180658_4_);
      }
   }

   public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
      super.updateEntityAfterFallOn(worldIn, entityIn);
      BlockPos position = entityIn.blockPosition();
      if (AllBlocks.WEIGHTED_EJECTOR.has(worldIn.getBlockState(position))) {
         if (entityIn.isAlive()) {
            if (!entityIn.isSuppressingBounce()) {
               if (!ItemHelper.fromItemEntity(entityIn).isEmpty()) {
                  SharedDepotBlockMethods.onLanded(worldIn, entityIn);
               } else {
                  Optional<EjectorBlockEntity> teProvider = this.getBlockEntityOptional(worldIn, position);
                  if (teProvider.isPresent()) {
                     EjectorBlockEntity ejectorBlockEntity = teProvider.get();
                     if (ejectorBlockEntity.getState() != EjectorBlockEntity.State.RETRACTING) {
                        if (!ejectorBlockEntity.powered) {
                           if (ejectorBlockEntity.launcher.getHorizontalDistance() != 0) {
                              if (entityIn.onGround()) {
                                 entityIn.setOnGround(false);
                                 Vec3 center = VecHelper.getCenterOf(position).add(0.0, 0.4375, 0.0);
                                 Vec3 positionVec = entityIn.position();
                                 double diff = center.distanceTo(positionVec);
                                 entityIn.setDeltaMovement(0.0, -0.125, 0.0);
                                 Vec3 vec = center.add(positionVec).scale(0.5);
                                 if (diff > 0.25) {
                                    entityIn.setPos(vec.x, vec.y, vec.z);
                                    return;
                                 }
                              }

                              ejectorBlockEntity.activate();
                              ejectorBlockEntity.notifyUpdate();
                              if (entityIn.level().isClientSide) {
                                 CatnipServices.NETWORK.sendToServer(new EjectorTriggerPacket(ejectorBlockEntity.getBlockPos()));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return AllItems.WRENCH.isIn(stack)
         ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
         : SharedDepotBlockMethods.onUse(stack, state, level, pos, player, hand, hitResult);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(HORIZONTAL_FACING)).getClockWise().getAxis();
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return this.getRotationAxis(state) == face.getAxis();
   }

   @Override
   public Class<EjectorBlockEntity> getBlockEntityClass() {
      return EjectorBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends EjectorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends EjectorBlockEntity>)AllBlockEntityTypes.WEIGHTED_EJECTOR.get();
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
      return SharedDepotBlockMethods.getComparatorInputOverride(blockState, worldIn, pos);
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }
}
