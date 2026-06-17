package com.simibubi.create.content.processing.basin;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BasinBlock extends Block implements IBE<BasinBlockEntity>, IWrenchable {
   public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;

   public BasinBlock(Properties p_i48440_1_) {
      super(p_i48440_1_);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(FACING, Direction.DOWN));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> p_206840_1_) {
      super.createBlockStateDefinition(p_206840_1_.add(new Property[]{FACING}));
   }

   public static boolean isBasin(LevelReader world, BlockPos pos) {
      return world.getBlockEntity(pos) instanceof BasinBlockEntity;
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      BlockEntity blockEntity = world.getBlockEntity(pos.above());
      return !(blockEntity instanceof BasinOperatingBlockEntity);
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      if (!context.getLevel().isClientSide) {
         this.withBlockEntityDo(context.getLevel(), context.getClickedPos(), bte -> bte.onWrenched(context.getClickedFace()));
      }

      return InteractionResult.SUCCESS;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return this.onBlockEntityUseItemOn(level, pos, be -> {
         if (!stack.isEmpty()) {
            if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, be)) {
               return ItemInteractionResult.SUCCESS;
            } else if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, be)) {
               return ItemInteractionResult.SUCCESS;
            } else if (!GenericItemEmptying.canItemBeEmptied(level, stack) && !GenericItemFilling.canItemBeFilled(level, stack)) {
               if (stack.getItem().equals(Items.SPONGE)) {
                  IFluidHandler fluidHandler = (IFluidHandler)level.getCapability(FluidHandler.BLOCK, pos, null);
                  if (fluidHandler != null) {
                     FluidStack drained = fluidHandler.drain(Integer.MAX_VALUE, FluidAction.EXECUTE);
                     if (!drained.isEmpty()) {
                        return ItemInteractionResult.SUCCESS;
                     }
                  }
               }

               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               return ItemInteractionResult.SUCCESS;
            }
         } else {
            IItemHandlerModifiable inv = be.itemCapability;
            if (inv == null) {
               inv = new ItemStackHandler(1);
            }

            boolean success = false;

            for (int slot = 0; slot < inv.getSlots(); slot++) {
               ItemStack stackInSlot = inv.getStackInSlot(slot);
               if (!stackInSlot.isEmpty()) {
                  player.getInventory().placeItemBackInInventory(stackInSlot);
                  inv.setStackInSlot(slot, ItemStack.EMPTY);
                  success = true;
               }
            }

            if (success) {
               level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F + level.getRandom().nextFloat());
            }

            be.onEmptied();
            return ItemInteractionResult.SUCCESS;
         }
      });
   }

   public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
      super.updateEntityAfterFallOn(worldIn, entityIn);
      if (worldIn.getBlockState(entityIn.blockPosition()).is(this)) {
         if (entityIn instanceof ItemEntity itemEntity) {
            if (entityIn.isAlive()) {
               this.withBlockEntityDo(worldIn, entityIn.blockPosition(), be -> {
                  ItemStack insertItem = ItemHandlerHelper.insertItem(be.inputInventory, itemEntity.getItem().copy(), false);
                  if (insertItem.isEmpty()) {
                     itemEntity.discard();
                  } else {
                     itemEntity.setItem(insertItem);
                  }
               });
            }
         }
      }
   }

   public VoxelShape getInteractionShape(BlockState p_199600_1_, BlockGetter p_199600_2_, BlockPos p_199600_3_) {
      return AllShapes.BASIN_RAYTRACE_SHAPE;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.BASIN_BLOCK_SHAPE;
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext ctx) {
      return ctx instanceof EntityCollisionContext && ((EntityCollisionContext)ctx).getEntity() instanceof ItemEntity
         ? AllShapes.BASIN_COLLISION_SHAPE
         : this.getShape(state, reader, pos, ctx);
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      IBE.onRemove(state, worldIn, pos, newState);
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
      return this.getBlockEntityOptional(worldIn, pos).map(BasinBlockEntity::getInputInventory).map(ItemHelper::calcRedstoneFromInventory).orElse(0);
   }

   @Override
   public Class<BasinBlockEntity> getBlockEntityClass() {
      return BasinBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends BasinBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends BasinBlockEntity>)AllBlockEntityTypes.BASIN.get();
   }

   public static boolean canOutputTo(BlockGetter world, BlockPos basinPos, Direction direction) {
      BlockPos neighbour = basinPos.relative(direction);
      BlockPos output = neighbour.below();
      BlockState blockState = world.getBlockState(neighbour);
      if (FunnelBlock.isFunnel(blockState)) {
         if (FunnelBlock.getFunnelFacing(blockState) == direction) {
            return false;
         }
      } else {
         if (!blockState.getCollisionShape(world, neighbour).isEmpty()) {
            return false;
         }

         if (world.getBlockEntity(output) instanceof BeltBlockEntity belt) {
            return belt.getSpeed() == 0.0F || belt.getMovementFacing() != direction.getOpposite();
         }
      }

      DirectBeltInputBehaviour directBeltInputBehaviour = BlockEntityBehaviour.get(world, output, DirectBeltInputBehaviour.TYPE);
      return directBeltInputBehaviour != null ? directBeltInputBehaviour.canInsertFromSide(direction) : false;
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }
}
