package com.simibubi.create.content.equipment.toolbox;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;

public class ToolboxBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, IBE<ToolboxBlockEntity> {
   protected final DyeColor color;
   public static final MapCodec<ToolboxBlock> CODEC = simpleCodec(p -> new ToolboxBlock(p, DyeColor.WHITE));

   public ToolboxBlock(Properties properties, DyeColor color) {
      super(properties);
      this.color = color;
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{BlockStateProperties.WATERLOGGED}).add(new Property[]{FACING}));
   }

   public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(worldIn, pos, state, placer, stack);
      if (!worldIn.isClientSide) {
         if (stack != null) {
            this.withBlockEntityDo(worldIn, pos, be -> {
               be.readInventory((ToolboxInventory)stack.get(AllDataComponents.TOOLBOX_INVENTORY));
               if (stack.has(AllDataComponents.TOOLBOX_UUID)) {
                  be.setUniqueId((UUID)stack.get(AllDataComponents.TOOLBOX_UUID));
               }

               if (stack.has(DataComponents.CUSTOM_NAME)) {
                  be.setCustomName(stack.getHoverName());
               }
            });
         }
      }
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moving) {
      if (state.hasBlockEntity() && (!newState.hasBlockEntity() || !(newState.getBlock() instanceof ToolboxBlock))) {
         world.removeBlockEntity(pos);
      }
   }

   public void attack(BlockState state, Level world, BlockPos pos, Player player) {
      if (!(player instanceof FakePlayer)) {
         if (!world.isClientSide) {
            this.withBlockEntityDo(world, pos, ToolboxBlockEntity::unequipTracked);
            if (world instanceof ServerLevel) {
               ItemStack cloneItemStack = this.getCloneItemStack(world, pos, state);
               this.withBlockEntityDo(world, pos, i -> cloneItemStack.applyComponents(i.collectComponents()));
               world.destroyBlock(pos, false);
               if (world.getBlockState(pos) != state) {
                  player.getInventory().placeItemBackInInventory(cloneItemStack);
               }
            }
         }
      }
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      ItemStack item = new ItemStack(this);
      Optional<ToolboxBlockEntity> blockEntityOptional = this.getBlockEntityOptional(level, pos);
      blockEntityOptional.map(tb -> (ToolboxInventory)item.set(AllDataComponents.TOOLBOX_INVENTORY, tb.inventory));
      blockEntityOptional.map(ToolboxBlockEntity::getUniqueId).ifPresent(uid -> item.set(AllDataComponents.TOOLBOX_UUID, uid));
      blockEntityOptional.map(ToolboxBlockEntity::getCustomName).ifPresent(name -> item.set(DataComponents.CUSTOM_NAME, name));
      return item;
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
      if ((Boolean)state.getValue(BlockStateProperties.WATERLOGGED)) {
         world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
      }

      return state;
   }

   public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
      return AllShapes.TOOLBOX.get((Direction)state.getValue(FACING));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player != null && !player.isCrouching()) {
         DyeColor color = DyeColor.getColor(stack);
         if (color != null && color != this.color) {
            if (level.isClientSide) {
               return ItemInteractionResult.SUCCESS;
            } else {
               BlockState newState = BlockHelper.copyProperties(state, AllBlocks.TOOLBOXES.get(color).getDefaultState());
               level.setBlockAndUpdate(pos, newState);
               return ItemInteractionResult.SUCCESS;
            }
         } else if (player instanceof FakePlayer) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         } else {
            this.withBlockEntityDo(level, pos, toolbox -> player.openMenu(toolbox, toolbox::sendToMenu));
            return ItemInteractionResult.SUCCESS;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState ifluidstate = context.getLevel().getFluidState(context.getClickedPos());
      return (BlockState)((BlockState)super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection().getOpposite()))
         .setValue(BlockStateProperties.WATERLOGGED, ifluidstate.getType() == Fluids.WATER);
   }

   @Override
   public Class<ToolboxBlockEntity> getBlockEntityClass() {
      return ToolboxBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends ToolboxBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ToolboxBlockEntity>)AllBlockEntityTypes.TOOLBOX.get();
   }

   public DyeColor getColor() {
      return this.color;
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
      return ItemHelper.calcRedstoneFromBlockEntity(this, pLevel, pPos);
   }

   @NotNull
   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
