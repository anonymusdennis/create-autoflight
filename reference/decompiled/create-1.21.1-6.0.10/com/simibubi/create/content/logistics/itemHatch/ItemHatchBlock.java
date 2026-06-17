package com.simibubi.create.content.logistics.itemHatch;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ItemHatchBlock extends HorizontalDirectionalBlock implements IBE<ItemHatchBlockEntity>, IWrenchable, ProperWaterloggedBlock {
   public static final MapCodec<ItemHatchBlock> CODEC = simpleCodec(ItemHatchBlock::new);
   public static final BooleanProperty OPEN = BooleanProperty.create("open");

   public ItemHatchBlock(Properties pProperties) {
      super(pProperties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(OPEN, false)).setValue(WATERLOGGED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{OPEN, FACING, WATERLOGGED}));
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState state = super.getStateForPlacement(pContext);
      if (state == null) {
         return state;
      } else {
         return pContext.getClickedFace().getAxis().isVertical()
            ? null
            : this.withWater((BlockState)((BlockState)state.setValue(FACING, pContext.getClickedFace().getOpposite())).setValue(OPEN, false), pContext);
      }
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
      this.updateWater(pLevel, pState, pPos);
      return pState;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (level.isClientSide()) {
         return ItemInteractionResult.SUCCESS;
      } else if (player instanceof FakePlayer) {
         return ItemInteractionResult.SUCCESS;
      } else {
         BlockEntity blockEntity = level.getBlockEntity(pos.relative((Direction)state.getValue(FACING)));
         if (blockEntity == null) {
            return ItemInteractionResult.FAIL;
         } else {
            IItemHandler targetInv = (IItemHandler)level.getCapability(ItemHandler.BLOCK, blockEntity.getBlockPos(), null);
            if (targetInv == null) {
               return ItemInteractionResult.FAIL;
            } else {
               FilteringBehaviour filter = BlockEntityBehaviour.get(level, pos, FilteringBehaviour.TYPE);
               if (filter == null) {
                  return ItemInteractionResult.FAIL;
               } else {
                  Inventory inventory = player.getInventory();
                  List<ItemStack> failedInsertions = new ArrayList<>();
                  boolean anyInserted = false;
                  boolean depositItemInHand = !player.isShiftKeyDown();
                  if (!depositItemInHand && stack.is(Items.TOOLS_WRENCH)) {
                     return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                  } else {
                     for (int i = 0; i < inventory.items.size(); i++) {
                        if (Inventory.isHotbarSlot(i) == depositItemInHand && (!depositItemInHand || i == inventory.selected)) {
                           ItemStack item = inventory.getItem(i);
                           if (!item.isEmpty()
                              && (item.getItem().canFitInsideContainerItems() || PackageItem.isPackage(item))
                              && (filter.getFilter().isEmpty() || filter.test(item))) {
                              ItemStack remainder = ItemHandlerHelper.insertItemStacked(targetInv, item, true);
                              if (remainder.getCount() != item.getCount()) {
                                 ItemStack extracted = inventory.removeItem(i, item.getCount() - remainder.getCount());
                                 remainder = ItemHandlerHelper.insertItemStacked(targetInv, extracted, false);
                                 anyInserted = true;
                                 if (!remainder.isEmpty()) {
                                    failedInsertions.add(remainder);
                                 }
                              }
                           }
                        }
                     }

                     failedInsertions.forEach(inventory::placeItemBackInInventory);
                     if (!anyInserted) {
                        return ItemInteractionResult.SUCCESS;
                     } else {
                        AllSoundEvents.ITEM_HATCH.playOnServer(level, pos);
                        level.setBlockAndUpdate(pos, (BlockState)state.setValue(OPEN, true));
                        level.scheduleTick(pos, this, 10);
                        CreateLang.translate(depositItemInHand ? "item_hatch.deposit_item" : "item_hatch.deposit_inventory").sendStatus(player);
                        return ItemInteractionResult.SUCCESS;
                     }
                  }
               }
            }
         }
      }
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.ITEM_HATCH.get(((Direction)pState.getValue(FACING)).getOpposite());
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if ((Boolean)pState.getValue(OPEN)) {
         pLevel.setBlockAndUpdate(pPos, (BlockState)pState.setValue(OPEN, false));
      }
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      IBE.onRemove(state, level, pos, newState);
   }

   @Override
   public Class<ItemHatchBlockEntity> getBlockEntityClass() {
      return ItemHatchBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends ItemHatchBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ItemHatchBlockEntity>)AllBlockEntityTypes.ITEM_HATCH.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
