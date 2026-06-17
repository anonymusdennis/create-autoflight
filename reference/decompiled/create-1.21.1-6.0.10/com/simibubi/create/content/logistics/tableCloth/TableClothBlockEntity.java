package com.simibubi.create.content.logistics.tableCloth;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.redstoneRequester.AutoRequestData;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.foundation.blockEntity.RemoveBlockEntityPacket;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

public class TableClothBlockEntity extends SmartBlockEntity implements TransformableBlockEntity {
   public AbstractComputerBehaviour computerBehaviour;
   public AutoRequestData requestData;
   public List<ItemStack> manuallyAddedItems = new ArrayList<>();
   public UUID owner;
   public Direction facing;
   public boolean sideOccluded;
   public FilteringBehaviour priceTag;
   private List<ItemStack> renderedItemsForShop;

   public TableClothBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.requestData = new AutoRequestData();
      this.owner = null;
      this.facing = Direction.SOUTH;
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(), (BlockEntityType)AllBlockEntityTypes.TABLE_CLOTH.get(), (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.priceTag = new TableClothFilteringBehaviour(this));
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   public List<ItemStack> getItemsForRender() {
      if (this.isShop()) {
         if (this.renderedItemsForShop == null) {
            this.renderedItemsForShop = this.requestData.encodedRequest().stacks().stream().map(b -> b.stack).limit(4L).toList();
         }

         return this.renderedItemsForShop;
      } else {
         return this.manuallyAddedItems;
      }
   }

   public void invalidateItemsForRender() {
      this.renderedItemsForShop = null;
   }

   public void notifyShopUpdate() {
      if (this.level instanceof ServerLevel serverLevel) {
         CatnipServices.NETWORK.sendToClientsTrackingChunk(serverLevel, new ChunkPos(this.worldPosition), new ShopUpdatePacket(this.worldPosition));
      }
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      BlockPos relativePos = this.worldPosition.relative(this.facing);
      this.sideOccluded = AllTags.AllBlockTags.TABLE_CLOTHS.matches(this.level.getBlockState(relativePos))
         || Block.isFaceFull(this.level.getBlockState(relativePos.below()).getOcclusionShape(this.level, relativePos.below()), this.facing.getOpposite());
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().inflate(1.0);
   }

   public boolean isShop() {
      return !this.requestData.encodedRequest().isEmpty();
   }

   public ItemInteractionResult use(Player player, BlockHitResult ray) {
      if (this.isShop()) {
         return this.useShop(player);
      } else {
         ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
         if (heldItem.isEmpty()) {
            if (this.manuallyAddedItems.isEmpty()) {
               return ItemInteractionResult.SUCCESS;
            } else {
               player.setItemInHand(InteractionHand.MAIN_HAND, this.manuallyAddedItems.remove(this.manuallyAddedItems.size() - 1));
               this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.5F, 1.0F);
               if (this.manuallyAddedItems.isEmpty() && !this.computerBehaviour.hasAttachedComputer()) {
                  this.level.setBlock(this.worldPosition, (BlockState)this.getBlockState().setValue(TableClothBlock.HAS_BE, false), 3);
                  if (this.level instanceof ServerLevel serverLevel) {
                     CatnipServices.NETWORK
                        .sendToClientsTrackingChunk(serverLevel, new ChunkPos(this.worldPosition), new RemoveBlockEntityPacket(this.worldPosition));
                  }
               } else {
                  this.notifyUpdate();
               }

               return ItemInteractionResult.SUCCESS;
            }
         } else if (this.manuallyAddedItems.size() >= 4) {
            return ItemInteractionResult.SUCCESS;
         } else {
            this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.5F, 1.0F);
            this.manuallyAddedItems.add(heldItem.copyWithCount(1));
            this.facing = player.getDirection().getOpposite();
            heldItem.shrink(1);
            if (heldItem.isEmpty()) {
               player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }

            this.notifyUpdate();
            return ItemInteractionResult.SUCCESS;
         }
      }
   }

   public boolean targetsPriceTag(Player player, BlockHitResult ray) {
      return this.priceTag != null
         && this.priceTag.mayInteract(player)
         && this.priceTag
            .getSlotPositioning()
            .testHit(this.level, this.worldPosition, this.getBlockState(), ray.getLocation().subtract(Vec3.atLowerCornerOf(this.worldPosition)));
   }

   public ItemInteractionResult useShop(Player player) {
      ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
      ItemStack prevListItem = ItemStack.EMPTY;
      boolean addOntoList = false;

      for (int i = 0; i < 9; i++) {
         ItemStack item = player.getInventory().getItem(i);
         if (AllItems.SHOPPING_LIST.isIn(item)) {
            prevListItem = item;
            addOntoList = true;
            player.getInventory().setItem(i, ItemStack.EMPTY);
         }
      }

      if (AllItems.SHOPPING_LIST.isIn(itemInHand)) {
         prevListItem = itemInHand;
         addOntoList = true;
      }

      if (!itemInHand.isEmpty() && !addOntoList) {
         CreateLang.translate("stock_keeper.shopping_list_empty_hand").sendStatus(player);
         AllSoundEvents.DENY.playOnServer(this.level, this.worldPosition, 0.5F, 1.0F);
         return ItemInteractionResult.SUCCESS;
      } else if (this.getPaymentItem().isEmpty()) {
         CreateLang.translate("stock_keeper.no_price_set").sendStatus(player);
         AllSoundEvents.DENY.playOnServer(this.level, this.worldPosition, 0.5F, 1.0F);
         return ItemInteractionResult.SUCCESS;
      } else {
         UUID tickerID = null;
         BlockPos tickerPos = this.requestData.targetOffset().offset(this.worldPosition);
         if (this.level.getBlockEntity(tickerPos) instanceof StockTickerBlockEntity stbe && stbe.isKeeperPresent()) {
            tickerID = stbe.behaviour.freqId;
         }

         int stockLevel = this.getStockLevelForTrade(ShoppingListItem.getList(prevListItem));
         if (tickerID == null) {
            CreateLang.translate("stock_keeper.keeper_missing").style(ChatFormatting.RED).sendStatus(player);
            AllSoundEvents.DENY.playOnServer(this.level, this.worldPosition, 0.5F, 1.0F);
            return ItemInteractionResult.SUCCESS;
         } else if (stockLevel == 0) {
            CreateLang.translate("stock_keeper.out_of_stock").style(ChatFormatting.RED).sendStatus(player);
            AllSoundEvents.DENY.playOnServer(this.level, this.worldPosition, 0.5F, 1.0F);
            if (!prevListItem.isEmpty()) {
               if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                  player.setItemInHand(InteractionHand.MAIN_HAND, prevListItem);
               } else {
                  player.getInventory().placeItemBackInInventory(prevListItem);
               }
            }

            return ItemInteractionResult.SUCCESS;
         } else {
            ShoppingListItem.ShoppingList list = new ShoppingListItem.ShoppingList(new ArrayList<>(), this.owner, tickerID);
            if (addOntoList) {
               ShoppingListItem.ShoppingList prevList = ShoppingListItem.getList(prevListItem).duplicate();
               if (this.owner.equals(prevList.shopOwner()) && tickerID.equals(prevList.shopNetwork())) {
                  list = prevList;
               } else {
                  addOntoList = false;
               }
            }

            if (list.getPurchases(this.worldPosition) >= stockLevel) {
               for (IntAttached<BlockPos> entry : list.purchases()) {
                  if (this.worldPosition.equals(entry.getValue())) {
                     entry.setFirst(Math.min(stockLevel, (Integer)entry.getFirst()));
                  }
               }

               CreateLang.translate("stock_keeper.limited_stock").style(ChatFormatting.RED).sendStatus(player);
            } else {
               AllSoundEvents.CONFIRM_2.playOnServer(this.level, this.worldPosition, 0.5F, 1.0F);
               ShoppingListItem.ShoppingList.Mutable mutable = new ShoppingListItem.ShoppingList.Mutable(list);
               mutable.addPurchases(this.worldPosition, 1);
               list = mutable.toImmutable();
               if (!addOntoList) {
                  CreateLang.translate("stock_keeper.use_list_to_add_purchases").color(15658734).sendStatus(player);
               }

               if (!addOntoList) {
                  this.level.playSound(null, this.worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 1.5F);
               }
            }

            ItemStack newListItem = ShoppingListItem.saveList(AllItems.SHOPPING_LIST.asStack(), list, this.requestData.encodedTargetAddress());
            if (player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
               player.setItemInHand(InteractionHand.MAIN_HAND, newListItem);
            } else {
               player.getInventory().placeItemBackInInventory(newListItem);
            }

            return ItemInteractionResult.SUCCESS;
         }
      }
   }

   public int getStockLevelForTrade(@Nullable ShoppingListItem.ShoppingList otherPurchases) {
      BlockPos tickerPos = this.requestData.targetOffset().offset(this.worldPosition);
      if (this.level.getBlockEntity(tickerPos) instanceof StockTickerBlockEntity stbe) {
         Object var9 = null;
         if (this.level.isClientSide()) {
            if (stbe.getTicksSinceLastUpdate() > 15) {
               stbe.refreshClientStockSnapshot();
            }

            var9 = stbe.getLastClientsideStockSnapshotAsSummary();
         } else {
            var9 = stbe.getRecentSummary();
         }

         if (var9 == null) {
            return 0;
         } else {
            InventorySummary modifierSummary = new InventorySummary();
            if (otherPurchases != null) {
               modifierSummary = (InventorySummary)otherPurchases.bakeEntries(this.level, this.worldPosition).getFirst();
            }

            int smallestQuotient = Integer.MAX_VALUE;

            for (BigItemStack entry : this.requestData.encodedRequest().stacks()) {
               if (entry.count > 0) {
                  smallestQuotient = Math.min(
                     smallestQuotient, (((InventorySummary)var9).getCountOf(entry.stack) - modifierSummary.getCountOf(entry.stack)) / entry.count
                  );
               }
            }

            return smallestQuotient;
         }
      } else {
         return 0;
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.put("Items", NBTHelper.writeItemList(this.manuallyAddedItems, registries));
      tag.putInt("Facing", this.facing.get2DDataValue());
      tag.put("RequestData", (Tag)CatnipCodecUtils.encode(AutoRequestData.CODEC, registries, this.requestData).orElseThrow());
      if (this.owner != null) {
         tag.putUUID("OwnerUUID", this.owner);
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.manuallyAddedItems = NBTHelper.readItemList(tag.getList("Items", 10), registries);
      this.requestData = CatnipCodecUtils.decode(AutoRequestData.CODEC, registries, tag.get("RequestData")).orElse(new AutoRequestData());
      this.owner = tag.contains("OwnerUUID") ? tag.getUUID("OwnerUUID") : null;
      this.facing = Direction.from2DDataValue(Mth.positiveModulo(tag.getInt("Facing"), 4));
   }

   @Override
   public void destroy() {
      super.destroy();
      this.manuallyAddedItems
         .forEach(
            stack -> Containers.dropItemStack(
                  this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), stack
               )
         );
      this.manuallyAddedItems.clear();
   }

   public ItemStack getPaymentItem() {
      return this.priceTag.getFilter();
   }

   public int getPaymentAmount() {
      return this.priceTag.getFilter().isEmpty() ? 1 : this.priceTag.count;
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   @Override
   public void transform(BlockEntity blockEntity, StructureTransform transform) {
      this.facing = transform.mirrorFacing(this.facing);
      if (transform.rotationAxis == Axis.Y) {
         this.facing = transform.rotateFacing(this.facing);
      }

      this.notifyUpdate();
   }
}
