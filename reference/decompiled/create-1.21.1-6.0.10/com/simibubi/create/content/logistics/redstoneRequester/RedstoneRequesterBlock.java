package com.simibubi.create.content.logistics.redstoneRequester;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;

public class RedstoneRequesterBlock extends Block implements IBE<RedstoneRequesterBlockEntity>, IWrenchable {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

   public RedstoneRequesterBlock(Properties pProperties) {
      super(pProperties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{POWERED, AXIS}));
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = super.getStateForPlacement(pContext);
      return stateForPlacement == null
         ? null
         : (BlockState)((BlockState)stateForPlacement.setValue(AXIS, pContext.getHorizontalDirection().getAxis()))
            .setValue(POWERED, pContext.getLevel().hasNeighborSignal(pContext.getClickedPos()));
   }

   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   public boolean hasAnalogOutputSignal(BlockState pState) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
      RedstoneRequesterBlockEntity req = this.getBlockEntity(pLevel, pPos);
      return req != null && req.lastRequestSucceeded ? 15 : 0;
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      return this.onBlockEntityUse(level, pos, be -> be.use(player));
   }

   public static void programRequester(ServerPlayer player, StockTickerBlockEntity be, PackageOrderWithCrafts order, String address) {
      ItemStack stack = player.getMainHandItem();
      boolean isRequester = AllBlocks.REDSTONE_REQUESTER.isIn(stack);
      boolean isShopCloth = AllTags.AllItemTags.TABLE_CLOTHS.matches(stack);
      if (isRequester || isShopCloth) {
         String targetDim = player.level().dimension().location().toString();
         AutoRequestData autoRequestData = new AutoRequestData(order, address, be.getBlockPos(), targetDim, false);
         autoRequestData.writeToItem(BlockPos.ZERO, stack);
         if (isRequester) {
            CompoundTag beTag = ((CustomData)stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)).copyTag();
            beTag.putUUID("Freq", be.behaviour.freqId);
            BlockEntity.addEntityType(beTag, (BlockEntityType)AllBlockEntityTypes.REDSTONE_REQUESTER.get());
            stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beTag));
         }

         player.setItemInHand(InteractionHand.MAIN_HAND, stack);
      }
   }

   public static void appendRequesterTooltip(ItemStack pStack, List<Component> pTooltip) {
      if (pStack.has(AllDataComponents.AUTO_REQUEST_DATA)) {
         AutoRequestData data = (AutoRequestData)pStack.get(AllDataComponents.AUTO_REQUEST_DATA);

         for (BigItemStack entry : data.encodedRequest().stacks()) {
            pTooltip.add(entry.stack.getHoverName().copy().append(" x").append(String.valueOf(entry.count)).withStyle(ChatFormatting.GRAY));
         }

         CreateLang.translate("logistically_linked.tooltip_clear").style(ChatFormatting.DARK_GRAY).addTo(pTooltip);
      }
   }

   public void setPlacedBy(Level pLevel, BlockPos requesterPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      Player player = pPlacer instanceof Player ? (Player)pPlacer : null;
      this.withBlockEntityDo(pLevel, requesterPos, rrbe -> {
         AutoRequestData data = AutoRequestData.readFromItem(pLevel, player, requesterPos, pStack);
         if (data != null) {
            rrbe.encodedRequest = data.encodedRequest();
            rrbe.encodedTargetAdress = data.encodedTargetAddress();
         }
      });
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
      if (!pLevel.isClientSide()) {
         pLevel.setBlockAndUpdate(pPos, (BlockState)pState.setValue(POWERED, pLevel.hasNeighborSignal(pPos)));
         this.withBlockEntityDo(pLevel, pPos, RedstoneRequesterBlockEntity::onRedstonePowerChanged);
      }
   }

   @Override
   public Class<RedstoneRequesterBlockEntity> getBlockEntityClass() {
      return RedstoneRequesterBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends RedstoneRequesterBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends RedstoneRequesterBlockEntity>)AllBlockEntityTypes.REDSTONE_REQUESTER.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return (BlockState)pState.setValue(AXIS, pRotation.rotate(Direction.get(AxisDirection.POSITIVE, (Axis)pState.getValue(AXIS))).getAxis());
   }
}
