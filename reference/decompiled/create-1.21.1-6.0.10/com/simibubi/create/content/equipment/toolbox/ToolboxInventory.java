package com.simibubi.create.content.equipment.toolbox;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

public class ToolboxInventory extends ItemStackHandler {
   public static final int STACKS_PER_COMPARTMENT = 4;
   public static final Codec<ToolboxInventory> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               ItemSlots.maxSizeCodec(32).fieldOf("items").forGetter(ItemSlots::fromHandler),
               ItemStack.OPTIONAL_CODEC.listOf().fieldOf("filters").forGetter(toolbox -> toolbox.filters)
            )
            .apply(instance, ToolboxInventory::deserialize)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ToolboxInventory> STREAM_CODEC = StreamCodec.composite(
      ItemSlots.STREAM_CODEC,
      ItemSlots::fromHandler,
      CatnipStreamCodecBuilders.list(ItemStack.OPTIONAL_STREAM_CODEC),
      toolbox -> toolbox.filters,
      ToolboxInventory::deserialize
   );
   @Deprecated(
      since = "6.0.6",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public static final Codec<ToolboxInventory> BACKWARDS_COMPAT_CODEC = Codec.withAlternative(CODEC, ItemContainerContents.CODEC.xmap(i -> {
      ToolboxInventory inv = new ToolboxInventory(null);
      ItemHelper.fillItemStackHandler(i, inv);
      return inv;
   }, ItemHelper::containerContentsFromHandler));
   List<ItemStack> filters;
   boolean settling;
   private final ToolboxBlockEntity blockEntity;
   private boolean limitedMode;

   public ToolboxInventory(ToolboxBlockEntity be) {
      super(32);
      this.blockEntity = be;
      this.limitedMode = false;
      this.filters = new ArrayList<>();
      this.settling = false;

      for (int i = 0; i < 8; i++) {
         this.filters.add(ItemStack.EMPTY);
      }
   }

   public void inLimitedMode(Consumer<ToolboxInventory> action) {
      this.limitedMode = true;
      action.accept(this);
      this.limitedMode = false;
   }

   public void settle(int compartment) {
      int totalCount = 0;
      boolean valid = true;
      boolean shouldBeEmpty = false;
      ItemStack sample = ItemStack.EMPTY;

      for (int i = 0; i < 4; i++) {
         ItemStack stackInSlot = this.getStackInSlot(compartment * 4 + i);
         totalCount += stackInSlot.getCount();
         if (!shouldBeEmpty) {
            shouldBeEmpty = stackInSlot.isEmpty() || stackInSlot.getCount() != stackInSlot.getMaxStackSize();
         } else if (!stackInSlot.isEmpty()) {
            valid = false;
            sample = stackInSlot;
         }
      }

      if (!valid) {
         this.settling = true;
         if (!sample.isStackable()) {
            for (int ix = 0; ix < 4; ix++) {
               if (this.getStackInSlot(compartment * 4 + ix).isEmpty()) {
                  for (int j = ix + 1; j < 4; j++) {
                     ItemStack stackInSlot = this.getStackInSlot(compartment * 4 + j);
                     if (!stackInSlot.isEmpty()) {
                        this.setStackInSlot(compartment * 4 + ix, stackInSlot);
                        this.setStackInSlot(compartment * 4 + j, ItemStack.EMPTY);
                        break;
                     }
                  }
               }
            }
         } else {
            for (int ixx = 0; ixx < 4; ixx++) {
               ItemStack copy = totalCount <= 0 ? ItemStack.EMPTY : sample.copyWithCount(Math.min(totalCount, sample.getMaxStackSize()));
               this.setStackInSlot(compartment * 4 + ixx, copy);
               totalCount -= copy.getCount();
            }
         }

         this.settling = false;
         this.notifyUpdate();
      }
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      if (!stack.getItem().canFitInsideContainerItems()) {
         return false;
      } else if (slot >= 0 && slot < this.getSlots()) {
         int compartment = slot / 4;
         ItemStack filter = this.filters.get(compartment);
         if (this.limitedMode && filter.isEmpty()) {
            return false;
         } else {
            return !filter.isEmpty() && !canItemsShareCompartment(filter, stack) ? false : super.isItemValid(slot, stack);
         }
      } else {
         return false;
      }
   }

   public void setStackInSlot(int slot, ItemStack stack) {
      super.setStackInSlot(slot, stack);
      int compartment = slot / 4;
      if (!stack.isEmpty() && this.filters.get(compartment).isEmpty()) {
         this.filters.set(compartment, stack.copyWithCount(1));
         this.notifyUpdate();
      }
   }

   @NotNull
   public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
      ItemStack insertItem = super.insertItem(slot, stack, simulate);
      if (insertItem.getCount() != stack.getCount()) {
         int compartment = slot / 4;
         if (!stack.isEmpty() && this.filters.get(compartment).isEmpty()) {
            this.filters.set(compartment, stack.copyWithCount(1));
            this.notifyUpdate();
         }
      }

      return insertItem;
   }

   @NotNull
   public CompoundTag serializeNBT(@NotNull Provider registries) {
      CompoundTag compound = super.serializeNBT(registries);
      compound.put("Compartments", NBTHelper.writeItemList(this.filters, registries));
      return compound;
   }

   protected void onContentsChanged(int slot) {
      if (!this.settling && (this.blockEntity == null || !this.blockEntity.getLevel().isClientSide)) {
         this.settle(slot / 4);
      }

      this.notifyUpdate();
      super.onContentsChanged(slot);
   }

   public void deserializeNBT(@NotNull Provider registries, CompoundTag nbt) {
      this.filters = NBTHelper.readItemList(nbt.getList("Compartments", 10), registries);
      if (this.filters.size() != 8) {
         this.filters.clear();

         for (int i = 0; i < 8; i++) {
            this.filters.add(ItemStack.EMPTY);
         }
      }

      super.deserializeNBT(registries, nbt);
   }

   public ItemStack distributeToCompartment(@NotNull ItemStack stack, int compartment, boolean simulate) {
      if (stack.isEmpty()) {
         return stack;
      } else if (this.filters.get(compartment).isEmpty()) {
         return stack;
      } else {
         for (int i = 3; i >= 0; i--) {
            int slot = compartment * 4 + i;
            stack = this.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
               return ItemStack.EMPTY;
            }
         }

         return stack;
      }
   }

   public ItemStack takeFromCompartment(int amount, int compartment, boolean simulate) {
      if (amount == 0) {
         return ItemStack.EMPTY;
      } else {
         int remaining = amount;
         ItemStack lastValid = ItemStack.EMPTY;

         for (int i = 3; i >= 0; i--) {
            int slot = compartment * 4 + i;
            ItemStack extracted = this.extractItem(slot, remaining, simulate);
            remaining -= extracted.getCount();
            if (!extracted.isEmpty()) {
               lastValid = extracted;
            }

            if (remaining == 0) {
               return lastValid.copyWithCount(amount);
            }
         }

         return remaining == amount ? ItemStack.EMPTY : lastValid.copyWithCount(amount - remaining);
      }
   }

   public static ItemStack cleanItemNBT(ItemStack stack) {
      if (AllItems.BELT_CONNECTOR.isIn(stack)) {
         stack.remove(AllDataComponents.BELT_FIRST_SHAFT);
      }

      return stack;
   }

   public static boolean canItemsShareCompartment(ItemStack stack1, ItemStack stack2) {
      if (!stack1.isStackable() && !stack2.isStackable() && stack1.isDamageableItem() && stack2.isDamageableItem()) {
         return stack1.getItem() == stack2.getItem();
      } else {
         return AllItems.BELT_CONNECTOR.isIn(stack1) && AllItems.BELT_CONNECTOR.isIn(stack2) ? true : ItemStack.isSameItemSameComponents(stack1, stack2);
      }
   }

   private void notifyUpdate() {
      if (this.blockEntity != null) {
         this.blockEntity.notifyUpdate();
      }
   }

   private static ToolboxInventory deserialize(ItemSlots slots, List<ItemStack> filters) {
      ToolboxInventory inventory = new ToolboxInventory(null);
      inventory.settling = true;
      slots.forEach(inventory::setStackInSlot);
      inventory.settling = false;
      inventory.filters = new ArrayList<>(filters);
      return inventory;
   }

   public final boolean equals(Object o) {
      return !(o instanceof ToolboxInventory that)
         ? false
         : this.settling == that.settling
            && this.limitedMode == that.limitedMode
            && this.filters.equals(that.filters)
            && Objects.equals(this.blockEntity, that.blockEntity);
   }

   public int hashCode() {
      int result = this.filters.hashCode();
      result = 31 * result + Boolean.hashCode(this.settling);
      result = 31 * result + Objects.hashCode(this.blockEntity);
      return 31 * result + Boolean.hashCode(this.limitedMode);
   }
}
