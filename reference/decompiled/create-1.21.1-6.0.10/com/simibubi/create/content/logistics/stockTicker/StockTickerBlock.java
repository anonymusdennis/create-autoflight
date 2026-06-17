package com.simibubi.create.content.logistics.stockTicker;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
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

public class StockTickerBlock extends HorizontalDirectionalBlock implements IBE<StockTickerBlockEntity>, IWrenchable {
   public static final MapCodec<StockTickerBlock> CODEC = simpleCodec(StockTickerBlock::new);

   public StockTickerBlock(Properties pProperties) {
      super(pProperties);
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Direction facing = pContext.getHorizontalDirection().getOpposite();
      boolean reverse = pContext.getPlayer() != null && pContext.getPlayer().isShiftKeyDown();
      return (BlockState)super.getStateForPlacement(pContext).setValue(FACING, reverse ? facing.getOpposite() : facing);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{FACING}));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return stack.getItem() instanceof LogisticallyLinkedBlockItem
         ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
         : this.onBlockEntityUseItemOn(level, pos, stbe -> {
            if (!stbe.behaviour.mayInteractMessage(player)) {
               return ItemInteractionResult.SUCCESS;
            } else if (!level.isClientSide() && !stbe.receivedPayments.isEmpty()) {
               for (int i = 0; i < stbe.receivedPayments.getSlots(); i++) {
                  player.getInventory()
                     .placeItemBackInInventory(stbe.receivedPayments.extractItem(i, stbe.receivedPayments.getStackInSlot(i).getCount(), false));
               }

               AllSoundEvents.playItemPickup(player);
               return ItemInteractionResult.SUCCESS;
            } else {
               if (player instanceof ServerPlayer sp) {
                  if (stbe.isKeeperPresent()) {
                     sp.openMenu(stbe.new CategoryMenuProvider(), stbe.getBlockPos());
                  } else {
                     CreateLang.translate("stock_ticker.keeper_missing").sendStatus(player);
                  }
               }

               return ItemInteractionResult.SUCCESS;
            }
         });
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.STOCK_TICKER;
   }

   @OnlyIn(Dist.CLIENT)
   public PartialModel getHat(LevelAccessor level, BlockPos pos, LivingEntity keeper) {
      return AllPartialModels.LOGISTICS_HAT;
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
   }

   @Override
   public Class<StockTickerBlockEntity> getBlockEntityClass() {
      return StockTickerBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends StockTickerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends StockTickerBlockEntity>)AllBlockEntityTypes.STOCK_TICKER.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
