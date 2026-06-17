package dev.simulated_team.simulated.content.blocks.nav_table;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StrictNbtStackRequirement;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class NavTableBlock extends DirectionalBlock implements IBE<NavTableBlockEntity>, IWrenchable, CommonRedstoneBlock, SpecialBlockItemRequirement {
   public static final MapCodec<NavTableBlock> CODEC = simpleCodec(NavTableBlock::new);

   public NavTableBlock(Properties pProperties) {
      super(pProperties);
   }

   protected MapCodec<? extends DirectionalBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{FACING}));
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction clickedFace = context.getClickedFace();
      return (BlockState)super.getStateForPlacement(context).setValue(FACING, clickedFace);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      if (level.isClientSide() && this.canSwitchStacks(itemStack, level, blockPos)) {
         return ItemInteractionResult.SUCCESS;
      } else {
         return this.switchStacks(level, blockPos, player, interactionHand)
            ? ItemInteractionResult.CONSUME
            : super.useItemOn(itemStack, blockState, level, blockPos, player, interactionHand, blockHitResult);
      }
   }

   private boolean canSwitchStacks(ItemStack heldStack, Level level, BlockPos pos) {
      NavTableBlockEntity blockEntity = (NavTableBlockEntity)level.getBlockEntity(pos);
      return blockEntity == null ? false : heldStack.has(SimDataComponents.TARGET) || !blockEntity.getHeldItem().isEmpty() && heldStack.isEmpty();
   }

   private boolean switchStacks(Level level, BlockPos pos, Player player, InteractionHand hand) {
      boolean passed = false;
      ItemStack heldItem = player.getItemInHand(hand);
      NavigationTarget newTarget = NavigationTarget.ofStack(heldItem);
      if (heldItem.isEmpty() || newTarget != null) {
         this.withBlockEntityDo(level, pos, nav -> {
            ContainerSlot slot = nav.inventory.slot;
            ItemStack extract = slot.getStack().copy();
            ItemStack insert = heldItem.copyWithCount(1);
            if (!extract.isEmpty()) {
               NavigationTarget oldTarget = nav.getNavTableItem();
               if (oldTarget != null) {
                  oldTarget.onExtract(extract, nav, player);
               }
            }

            if (newTarget != null) {
               newTarget.onInsert(insert, nav, player);
            }

            slot.setStack(insert);
            if (!player.hasInfiniteMaterials()) {
               heldItem.shrink(1);
            }

            if (!extract.isEmpty()) {
               player.getInventory().placeItemBackInInventory(extract.copy());
            }

            ItemStack newSlotItem = slot.getStack();
            nav.setChanged();
            nav.sendData();
            float pitch = 0.8F + level.random.nextFloat() * 0.4F;
            float volume = 0.75F;
            if (extract.isEmpty() && !newSlotItem.isEmpty()) {
               level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 0.75F, pitch);
            } else if (!extract.isEmpty() && newSlotItem.isEmpty()) {
               level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.75F, pitch);
            } else if (!extract.isEmpty()) {
               level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 0.75F, pitch);
            }
         });
         passed = true;
      }

      return passed;
   }

   public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      NavTableBlockEntity be = (NavTableBlockEntity)this.getBlockEntity(level, pos);
      return be != null && direction.getAxis() != ((Direction)state.getValue(FACING)).getAxis() ? be.getRedstoneStrength(direction) : 0;
   }

   public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      if (direction.getAxis() == ((Direction)state.getValue(FACING)).getAxis()) {
         return 0;
      } else {
         return ((Direction)state.getValue(FACING)).getAxis().isHorizontal() && direction == Direction.DOWN ? this.getSignal(state, level, pos, direction) : 0;
      }
   }

   @Override
   public boolean commonCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   @Override
   public boolean commonConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      return direction == null ? false : direction.getAxis() != ((Direction)state.getValue(FACING)).getAxis();
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
      this.withBlockEntityDo(level, pos, NavTableBlockEntity::dropHeldItem);
      IBE.onRemove(state, level, pos, newState);
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return SimBlockShapes.NAV_TABLE.get((Direction)state.getValue(FACING));
   }

   public Class<NavTableBlockEntity> getBlockEntityClass() {
      return NavTableBlockEntity.class;
   }

   public BlockEntityType<? extends NavTableBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends NavTableBlockEntity>)SimBlockEntityTypes.NAVIGATION_TABLE.get();
   }

   public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
      ItemStack tableStack = SimBlocks.NAVIGATION_TABLE.asStack();
      if (blockEntity instanceof NavTableBlockEntity ntbe) {
         ItemStack heldItem = ntbe.getHeldItem();
         if (!heldItem.isEmpty()) {
            return new ItemRequirement(
               List.of(new StackRequirement(tableStack, ItemUseType.CONSUME), new StrictNbtStackRequirement(heldItem, ItemUseType.CONSUME))
            );
         }
      }

      return new ItemRequirement(ItemUseType.CONSUME, tableStack);
   }
}
