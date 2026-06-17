package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.animatedContainer.AnimatedContainerBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public abstract class PackagePortBlockEntity extends SmartBlockEntity implements MenuProvider, Clearable {
   public boolean acceptsPackages;
   public String addressFilter = "";
   public PackagePortTarget target;
   public SmartInventory inventory;
   protected AnimatedContainerBehaviour<PackagePortMenu> openTracker;
   protected IItemHandler itemHandler;

   public PackagePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.acceptsPackages = true;
      this.inventory = new SmartInventory(18, this, (slot, stack) -> PackageItem.isPackage(stack));
      this.itemHandler = new PackagePortAutomationInventoryWrapper(this.inventory, this);
   }

   public boolean isBackedUp() {
      for (int i = 0; i < this.inventory.getSlots(); i++) {
         if (this.inventory.getStackInSlot(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void filterChanged() {
      if (this.target != null) {
         this.target.deregister(this, this.level, this.worldPosition);
         this.target.register(this, this.level, this.worldPosition);
      }
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (this.target != null) {
         this.target.register(this, this.level, this.worldPosition);
      }
   }

   public String getFilterString() {
      return this.acceptsPackages ? this.addressFilter : null;
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      if (this.target != null) {
         tag.put("Target", (Tag)CatnipCodecUtils.encode(PackagePortTarget.CODEC, registries, this.target).orElseThrow());
      }

      tag.putString("AddressFilter", this.addressFilter);
      tag.putBoolean("AcceptsPackages", this.acceptsPackages);
      tag.put("Inventory", this.inventory.serializeNBT(registries));
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
      PackagePortTarget prevTarget = this.target;
      this.target = (PackagePortTarget)CatnipCodecUtils.decodeOrNull(PackagePortTarget.CODEC, registries, tag.getCompound("Target"));
      this.addressFilter = tag.getString("AddressFilter");
      this.acceptsPackages = tag.getBoolean("AcceptsPackages");
      if (clientPacket && prevTarget != this.target) {
         this.invalidateRenderBoundingBox();
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
   }

   public void clearContent() {
      this.inventory.clearContent();
   }

   @Override
   public void destroy() {
      if (this.target != null) {
         this.target.deregister(this, this.level, this.worldPosition);
      }

      super.destroy();

      for (int i = 0; i < this.inventory.getSlots(); i++) {
         this.drop(this.inventory.getStackInSlot(i));
      }
   }

   public void drop(ItemStack box) {
      if (!box.isEmpty()) {
         Block.popResource(this.level, this.worldPosition, box);
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.openTracker = new AnimatedContainerBehaviour(this, PackagePortMenu.class));
      this.openTracker.onOpenChanged(this::onOpenChange);
   }

   protected abstract void onOpenChange(boolean var1);

   public ItemInteractionResult use(Player player) {
      if (player == null || player.isCrouching()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (player instanceof FakePlayer) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         ItemStack mainHandItem = player.getMainHandItem();
         boolean clipboard = AllBlocks.CLIPBOARD.isIn(mainHandItem);
         if (this.level.isClientSide) {
            if (!clipboard) {
               this.onOpenedManually();
            }

            return ItemInteractionResult.SUCCESS;
         } else if (clipboard) {
            this.addAddressToClipboard(player, mainHandItem);
            return ItemInteractionResult.SUCCESS;
         } else {
            player.openMenu(this, this.worldPosition);
            return ItemInteractionResult.SUCCESS;
         }
      }
   }

   protected void onOpenedManually() {
   }

   private void addAddressToClipboard(Player player, ItemStack mainHandItem) {
      if (this.addressFilter != null && !this.addressFilter.isBlank()) {
         ClipboardContent clipboard = (ClipboardContent)mainHandItem.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
         List<List<ClipboardEntry>> list = ClipboardEntry.readAll(clipboard);

         for (List<ClipboardEntry> page : list) {
            for (ClipboardEntry entry : page) {
               String existing = entry.text.getString();
               if (existing.equals("#" + this.addressFilter) || existing.equals("# " + this.addressFilter)) {
                  return;
               }
            }
         }

         List<ClipboardEntry> page = null;

         for (List<ClipboardEntry> freePage : list) {
            if (freePage.size() <= 11) {
               page = freePage;
               break;
            }
         }

         if (page == null) {
            page = new ArrayList<>();
            list.add(page);
         }

         page.add(new ClipboardEntry(false, Component.literal("#" + this.addressFilter)));
         player.displayClientMessage(CreateLang.translate("clipboard.address_added", this.addressFilter).component(), true);
         clipboard = clipboard.setPages(list).setType(ClipboardOverrides.ClipboardType.WRITTEN);
         mainHandItem.set(AllDataComponents.CLIPBOARD_CONTENT, clipboard);
      }
   }

   public Component getDisplayName() {
      return Component.empty();
   }

   public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
      return PackagePortMenu.create(pContainerId, pPlayerInventory, this);
   }

   public int getComparatorOutput() {
      return ItemHandlerHelper.calcRedstoneFromInventory(this.inventory);
   }
}
