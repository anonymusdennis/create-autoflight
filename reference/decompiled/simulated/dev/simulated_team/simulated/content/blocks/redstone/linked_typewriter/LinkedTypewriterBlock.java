package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler;
import com.simibubi.create.content.redstone.link.controller.LinkedControllerClientHandler.Mode;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.service.SimMenuService;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;

public class LinkedTypewriterBlock extends HorizontalDirectionalBlock implements IBE<LinkedTypewriterBlockEntity>, IWrenchable {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final MapCodec<LinkedTypewriterBlock> CODEC = simpleCodec(LinkedTypewriterBlock::new);
   public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

   public LinkedTypewriterBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, false));
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED}).add(new Property[]{FACING});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction dir = pContext.getHorizontalDirection().getOpposite();

      assert pContext.getPlayer() != null;

      return (BlockState)this.defaultBlockState().setValue(HORIZONTAL_FACING, pContext.getPlayer().isShiftKeyDown() ? dir.getOpposite() : dir);
   }

   public Class<LinkedTypewriterBlockEntity> getBlockEntityClass() {
      return LinkedTypewriterBlockEntity.class;
   }

   public BlockEntityType<? extends LinkedTypewriterBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends LinkedTypewriterBlockEntity>)SimBlockEntityTypes.LINKED_TYPEWRITER.get();
   }

   public VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext context) {
      return SimBlockShapes.LINKED_TYPEWRITER.get((Direction)state.getValue(HORIZONTAL_FACING));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      ItemStack heldItem = player.getItemInHand(interactionHand);
      Item linkedControllerItem = AllItems.LINKED_CONTROLLER.asItem();
      if (!player.getMainHandItem().is(linkedControllerItem) && !player.getOffhandItem().is(linkedControllerItem)) {
         if (heldItem.isEmpty() && interactionHand == InteractionHand.MAIN_HAND) {
            MutableBoolean success = new MutableBoolean(false);
            this.withBlockEntityDo(level, blockPos, be -> {
               UUID uuid = player.getUUID();
               if (player.isShiftKeyDown() && be.checkAndStartUsing(uuid)) {
                  if (!level.isClientSide) {
                     this.displayScreen(be, player);
                  } else {
                     LinkedTypewriterInteractionHandler.setMode(LinkedTypewriterInteractionHandler.Mode.SCREEN_BINDING);
                  }

                  success.setTrue();
               } else if (be.checkAndStartUsing(uuid)) {
                  success.setTrue();
               } else {
                  if (be.checkUser(uuid)) {
                     be.disconnectUser();
                     success.setTrue();
                  }
               }
            });
            if (success.getValue()) {
               return ItemInteractionResult.SUCCESS;
            }
         }

         return super.useItemOn(itemStack, blockState, level, blockPos, player, interactionHand, blockHitResult);
      } else {
         if (level.isClientSide) {
            ItemStack item = player.getMainHandItem().is(linkedControllerItem) ? player.getMainHandItem() : player.getOffhandItem();
            player.displayClientMessage(SimLang.translate("linked_typewriter.linked_controller_copy").component(), true);
            LinkedTypewriterInteractionHandler.sendLinkedControllerData(level, blockPos, item);
            LinkedControllerClientHandler.MODE = Mode.IDLE;
         }

         return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof LinkedTypewriterBlockEntity typewriter) {
         return typewriter.isInUse() ? 15 : 0;
      } else {
         return 0;
      }
   }

   public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      Player player = context.getPlayer();
      if (world instanceof ServerLevel && player != null && player.isCreative()) {
         Block.getDrops(state, (ServerLevel)world, pos, world.getBlockEntity(pos), player, context.getItemInHand())
            .forEach(itemStack -> player.getInventory().placeItemBackInInventory(itemStack));
      }

      return super.onSneakWrenched(state, context);
   }

   public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
      ItemStack itemStack = super.getCloneItemStack(level, pos, state);
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity != null) {
         BlockItem.setBlockEntityData(itemStack, blockEntity.getType(), blockEntity.saveWithoutMetadata(level.registryAccess()));
         if (blockEntity.components().has(DataComponents.CUSTOM_NAME)) {
            itemStack.set(DataComponents.CUSTOM_NAME, (Component)blockEntity.components().get(DataComponents.CUSTOM_NAME));
         }
      }

      return itemStack;
   }

   public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
      assert level != null;

      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity != null
         && !level.isClientSide
         && player.isCreative()
         && blockEntity instanceof LinkedTypewriterBlockEntity linkedTypewriterBlockEntity
         && (
            !linkedTypewriterBlockEntity.getTypewriterEntries().getKeyMap().isEmpty()
               || linkedTypewriterBlockEntity.components().has(DataComponents.CUSTOM_NAME)
         )) {
         Containers.dropItemStack(level, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), this.getCloneItemStack(level, pos, state));
      }

      return super.playerWillDestroy(level, pos, state, player);
   }

   @NotNull
   public List<ItemStack> getDrops(@NotNull BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
      BlockEntity blockEntity = (BlockEntity)params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (blockEntity instanceof LinkedTypewriterBlockEntity typewriter) {
         ItemStack itemStack = new ItemStack(this);
         typewriter.saveToItem(itemStack, params.getLevel().registryAccess());
         params.withDynamicDrop(ShulkerBoxBlock.CONTENTS, consumer -> itemStack.copy());
         return ImmutableList.of(itemStack);
      } else {
         return super.getDrops(state, params);
      }
   }

   protected void displayScreen(LinkedTypewriterBlockEntity be, Player player) {
      SimMenuService.INSTANCE.openScreen((ServerPlayer)player, be, be::sendToMenu);
   }
}
