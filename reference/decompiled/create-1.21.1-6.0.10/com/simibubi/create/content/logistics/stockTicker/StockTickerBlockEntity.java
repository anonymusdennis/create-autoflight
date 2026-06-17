package com.simibubi.create.content.logistics.stockTicker;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.WiFiParticle;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class StockTickerBlockEntity extends StockCheckingBlockEntity implements IHaveHoveringInformation, Clearable {
   public AbstractComputerBehaviour computerBehaviour;
   protected List<List<BigItemStack>> lastClientsideStockSnapshot;
   protected InventorySummary lastClientsideStockSnapshotAsSummary;
   protected List<BigItemStack> newlyReceivedStockSnapshot;
   protected String previouslyUsedAddress = "";
   protected int activeLinks;
   protected int ticksSinceLastUpdate;
   protected List<ItemStack> categories;
   protected Map<UUID, List<Integer>> hiddenCategoriesByPlayer;
   protected SmartInventory receivedPayments = new SmartInventory(27, this, 64, false);

   public StockTickerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.categories = new ArrayList<>();
      this.hiddenCategoriesByPlayer = new HashMap<>();
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.STOCK_TICKER.get(), (be, context) -> be.receivedPayments);
      if (Mods.COMPUTERCRAFT.isLoaded()) {
         event.registerBlockEntity(
            PeripheralCapability.get(),
            (BlockEntityType)AllBlockEntityTypes.STOCK_TICKER.get(),
            (be, context) -> be.computerBehaviour.getPeripheralCapability()
         );
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(this.computerBehaviour = ComputerCraftProxy.behaviour(this));
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.computerBehaviour.removePeripheral();
   }

   public void refreshClientStockSnapshot() {
      this.ticksSinceLastUpdate = 0;
      CatnipServices.NETWORK.sendToServer(new LogisticalStockRequestPacket(this.worldPosition));
   }

   public IItemHandler getReceivedPaymentsHandler() {
      return this.receivedPayments;
   }

   public List<List<BigItemStack>> getClientStockSnapshot() {
      return this.lastClientsideStockSnapshot;
   }

   public InventorySummary getLastClientsideStockSnapshotAsSummary() {
      return this.lastClientsideStockSnapshotAsSummary;
   }

   public int getTicksSinceLastUpdate() {
      return this.ticksSinceLastUpdate;
   }

   @Override
   public boolean broadcastPackageRequest(
      LogisticallyLinkedBehaviour.RequestType type, PackageOrderWithCrafts order, IdentifiedInventory ignoredHandler, String address
   ) {
      boolean result = super.broadcastPackageRequest(type, order, ignoredHandler, address);
      this.previouslyUsedAddress = address;
      this.notifyUpdate();
      return result;
   }

   @Override
   public InventorySummary getRecentSummary() {
      InventorySummary recentSummary = super.getRecentSummary();
      int contributingLinks = recentSummary.contributingLinks;
      if (this.activeLinks != contributingLinks && !this.isRemoved()) {
         this.activeLinks = contributingLinks;
         this.sendData();
      }

      return recentSummary;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide()) {
         if (this.ticksSinceLastUpdate < 100) {
            this.ticksSinceLastUpdate++;
         }
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putString("PreviousAddress", this.previouslyUsedAddress);
      tag.put("ReceivedPayments", this.receivedPayments.serializeNBT(registries));
      tag.put("Categories", NBTHelper.writeItemList(this.categories, registries));
      tag.put("HiddenCategories", NBTHelper.writeCompoundList(this.hiddenCategoriesByPlayer.entrySet(), e -> {
         CompoundTag c = new CompoundTag();
         c.putUUID("Id", (UUID)e.getKey());
         c.putIntArray("Indices", (List)e.getValue());
         return c;
      }));
      if (clientPacket) {
         tag.putInt("ActiveLinks", this.activeLinks);
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.previouslyUsedAddress = tag.getString("PreviousAddress");
      this.receivedPayments.deserializeNBT(registries, tag.getCompound("ReceivedPayments"));
      this.categories = NBTHelper.readItemList(tag.getList("Categories", 10), registries);
      this.categories.removeIf(stack -> !stack.isEmpty() && !(stack.getItem() instanceof FilterItem));
      this.hiddenCategoriesByPlayer.clear();
      NBTHelper.iterateCompoundList(
         tag.getList("HiddenCategories", 10), c -> this.hiddenCategoriesByPlayer.put(c.getUUID("Id"), IntStream.of(c.getIntArray("Indices")).boxed().toList())
      );
      if (clientPacket) {
         this.activeLinks = tag.getInt("ActiveLinks");
      }
   }

   public void receiveStockPacket(List<BigItemStack> stacks, boolean endOfTransmission) {
      if (this.newlyReceivedStockSnapshot == null) {
         this.newlyReceivedStockSnapshot = new ArrayList<>();
      }

      this.newlyReceivedStockSnapshot.addAll(stacks);
      if (endOfTransmission) {
         this.lastClientsideStockSnapshotAsSummary = new InventorySummary();
         this.lastClientsideStockSnapshot = new ArrayList<>();

         for (BigItemStack bigStack : this.newlyReceivedStockSnapshot) {
            this.lastClientsideStockSnapshotAsSummary.add(bigStack);
         }

         for (ItemStack filter : this.categories) {
            List<BigItemStack> inCategory = new ArrayList<>();
            if (!filter.isEmpty()) {
               FilterItemStack filterItemStack = FilterItemStack.of(filter);
               Iterator<BigItemStack> iterator = this.newlyReceivedStockSnapshot.iterator();

               while (iterator.hasNext()) {
                  BigItemStack bigStack = iterator.next();
                  if (filterItemStack.test(this.level, bigStack.stack)) {
                     inCategory.add(bigStack);
                     iterator.remove();
                  }
               }
            }

            this.lastClientsideStockSnapshot.add(inCategory);
         }

         List<BigItemStack> unsorted = new ArrayList<>(this.newlyReceivedStockSnapshot);
         this.lastClientsideStockSnapshot.add(unsorted);
         this.newlyReceivedStockSnapshot = null;
      }
   }

   public boolean isKeeperPresent() {
      for (int yOffset : Iterate.zeroAndOne) {
         for (Direction side : Iterate.horizontalDirections) {
            BlockPos seatPos = this.worldPosition.below(yOffset).relative(side);

            for (SeatEntity seatEntity : this.level.getEntitiesOfClass(SeatEntity.class, new AABB(seatPos))) {
               if (seatEntity.isVehicle()) {
                  return true;
               }
            }

            if (yOffset == 0 && AllBlockEntityTypes.HEATER.is(this.level.getBlockEntity(seatPos))) {
               return true;
            }
         }
      }

      return false;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (this.receivedPayments.isEmpty()) {
         return false;
      } else if (!this.behaviour.mayAdministrate(Minecraft.getInstance().player)) {
         return false;
      } else {
         CreateLang.translate("stock_ticker.contains_payments").style(ChatFormatting.WHITE).forGoggles(tooltip);
         InventorySummary summary = new InventorySummary();

         for (int i = 0; i < this.receivedPayments.getSlots(); i++) {
            summary.add(this.receivedPayments.getStackInSlot(i));
         }

         for (BigItemStack entry : summary.getStacksByCount()) {
            CreateLang.builder()
               .text(Component.translatable(entry.stack.getDescriptionId()).getString() + " x" + entry.count)
               .style(ChatFormatting.GREEN)
               .forGoggles(tooltip);
         }

         CreateLang.translate("stock_ticker.click_to_retrieve").style(ChatFormatting.GRAY).forGoggles(tooltip);
         return true;
      }
   }

   public void clearContent() {
      this.categories.clear();
      this.receivedPayments.clearContent();
   }

   @Override
   public void destroy() {
      ItemHelper.dropContents(this.level, this.worldPosition, this.receivedPayments);

      for (ItemStack filter : this.categories) {
         if (!filter.isEmpty() && filter.getItem() instanceof FilterItem) {
            Containers.dropItemStack(
               this.level, (double)this.worldPosition.getX(), (double)this.worldPosition.getY(), (double)this.worldPosition.getZ(), filter
            );
         }
      }

      super.destroy();
   }

   public void playEffect() {
      AllSoundEvents.STOCK_LINK.playAt(this.level, this.worldPosition, 1.0F, 1.0F, false);
      Vec3 vec3 = Vec3.atCenterOf(this.worldPosition);
      this.level.addParticle(new WiFiParticle.Data(), vec3.x, vec3.y, vec3.z, 1.0, 1.0, 1.0);
   }

   public class CategoryMenuProvider implements MenuProvider {
      public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
         return StockKeeperCategoryMenu.create(pContainerId, pPlayerInventory, StockTickerBlockEntity.this);
      }

      public Component getDisplayName() {
         return Component.empty();
      }
   }

   public class RequestMenuProvider implements MenuProvider {
      public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
         return StockKeeperRequestMenu.create(pContainerId, pPlayerInventory, StockTickerBlockEntity.this);
      }

      public Component getDisplayName() {
         return Component.empty();
      }
   }
}
