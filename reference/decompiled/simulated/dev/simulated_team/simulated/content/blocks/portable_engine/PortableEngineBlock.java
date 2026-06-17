package dev.simulated_team.simulated.content.blocks.portable_engine;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.service.SimItemService;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PortableEngineBlock extends HorizontalKineticBlock implements IBE<PortableEngineBlockEntity> {
   private static final int BURN_TIME_THRESHOLD = 200;
   protected final DyeColor color;

   public PortableEngineBlock(Properties properties, DyeColor color) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(BlockStateProperties.LIT, false));
      this.color = color;
   }

   public static boolean isLitState(BlockState blockState) {
      return (Boolean)blockState.getValue(BlockStateProperties.LIT);
   }

   public static Couple<Integer> getSpeedRange() {
      return Couple.create(32, 32);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{BlockStateProperties.LIT});
      super.createBlockStateDefinition(builder);
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.hasBlockEntity() && !SimBlocks.PORTABLE_ENGINES.contains(newState.getBlock())) {
         PortableEngineBlockEntity be = (PortableEngineBlockEntity)level.getBlockEntity(pos);
         if (be != null && !be.inventory.isEmpty()) {
            Containers.dropItemStack(level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), be.inventory.getItem(0));
         }

         level.removeBlockEntity(pos);
      }
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face == state.getValue(HORIZONTAL_FACING);
   }

   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(HORIZONTAL_FACING)).getAxis();
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction preferred = this.getPreferredHorizontalFacing(context);
      if (preferred == null || context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
         Direction horizontalDirection = context.getHorizontalDirection();
         return (BlockState)this.defaultBlockState()
            .setValue(
               HORIZONTAL_FACING, context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? horizontalDirection.getOpposite() : horizontalDirection
            );
      } else {
         return (BlockState)this.defaultBlockState().setValue(HORIZONTAL_FACING, preferred);
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack heldItem, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      PortableEngineBlockEntity be = (PortableEngineBlockEntity)level.getBlockEntity(blockPos);
      PortableEngineInventory inventory = be.inventory;
      ContainerSlot slot = inventory.slot;
      ItemStack currentItemStack = slot.getStack().copy();
      if (currentItemStack.isEmpty() && heldItem.isEmpty()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         DyeColor color = SimItemService.getDyeColor(heldItem);
         if (color != null) {
            if (!level.isClientSide) {
               level.playSound(null, blockPos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.1F - level.random.nextFloat() * 0.2F);
            }

            BlockState newState = BlockHelper.copyProperties(blockState, SimBlocks.PORTABLE_ENGINES.get(color).getDefaultState());
            level.setBlockAndUpdate(blockPos, newState);
            return ItemInteractionResult.SUCCESS;
         } else {
            if (AllItems.CREATIVE_BLAZE_CAKE.isIn(heldItem)) {
               if (!level.isClientSide) {
                  if (be.isCurrentFuelInfinite()) {
                     if (be.isSuperHeated()) {
                        be.setCurrentBurnTime(0);
                        be.setSuperHeated(false);
                     } else {
                        be.setSuperHeated(true);
                     }
                  } else {
                     be.setCurrentBurnTime(PortableEngineBlockEntity.INFINITE_THRESHOLD);
                  }
               }

               if (!player.hasInfiniteMaterials()) {
                  heldItem.shrink(1);
                  player.setItemInHand(interactionHand, heldItem);
               }
            } else {
               if (!heldItem.isEmpty() && !inventory.canInsertItem(ItemInfoWrapper.generateFromStack(heldItem))) {
                  return ItemInteractionResult.FAIL;
               }

               if (currentItemStack.isEmpty()) {
                  slot.setStack(heldItem);
                  player.setItemInHand(interactionHand, ItemStack.EMPTY);
                  if (be.getCurrentBurnTime() <= 0) {
                     SimStats.PORTABLE_ENGINES_FED.awardTo(player);
                  }
               } else if (ItemStack.isSameItem(heldItem, currentItemStack) && ItemStack.isSameItemSameComponents(heldItem, currentItemStack)) {
                  int targetAmount = currentItemStack.getCount() + heldItem.getCount();
                  targetAmount = Math.min(targetAmount, currentItemStack.getMaxStackSize());
                  int transferAmount = Math.min(targetAmount - currentItemStack.getCount(), heldItem.getCount());
                  if (transferAmount <= 0) {
                     return ItemInteractionResult.sidedSuccess(level.isClientSide);
                  }

                  slot.shrink((long)(-transferAmount));
                  heldItem.shrink(transferAmount);
                  player.setItemInHand(interactionHand, heldItem);
               } else {
                  slot.setStack(ItemStack.EMPTY);
                  player.getInventory().placeItemBackInInventory(currentItemStack);
                  level.playSound(null, blockPos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F + level.getRandom().nextFloat());
               }

               if (be.getTotalBurnTime() >= 720000 && !be.isTotalFuelInfinite()) {
                  SimAdvancements.THAT_SHOULD_DO_FOR_NOW.awardTo(player);
               }
            }

            if (!slot.isEmpty()) {
               SimAdvancements.STEAMLESS_ENGINE.awardTo(player);
            }

            be.notifyUpdate();
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         }
      }
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public static int analogPower(int burnTime) {
      return burnTime > 0 ? Math.min(burnTime / 200, 14) + 1 : 0;
   }

   public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      PortableEngineBlockEntity be = (PortableEngineBlockEntity)this.getBlockEntity(pLevel, pPos);
      int power = 0;
      if (be != null) {
         return be.isTotalFuelInfinite() ? 15 : analogPower(be.getTotalBurnTime());
      } else {
         return 0;
      }
   }

   public DyeColor getColor() {
      return this.color;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext ctx) {
      return SimBlockShapes.PORTABLE_ENGINE.get((Direction)pState.getValue(HORIZONTAL_FACING));
   }

   public Class<PortableEngineBlockEntity> getBlockEntityClass() {
      return PortableEngineBlockEntity.class;
   }

   public BlockEntityType<? extends PortableEngineBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends PortableEngineBlockEntity>)SimBlockEntityTypes.PORTABLE_ENGINE.get();
   }
}
