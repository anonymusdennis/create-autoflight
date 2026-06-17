package com.simibubi.create.content.logistics.crate;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreativeCrateMountedStorage extends MountedItemStorage {
   public static final MapCodec<CreativeCrateMountedStorage> CODEC = ItemStack.OPTIONAL_CODEC
      .xmap(CreativeCrateMountedStorage::new, storage -> storage.suppliedStack)
      .fieldOf("value");
   private final ItemStack suppliedStack;
   private final ItemStack cachedStackInSlot;

   protected CreativeCrateMountedStorage(MountedItemStorageType<?> type, ItemStack suppliedStack) {
      super(type);
      this.suppliedStack = suppliedStack;
      this.cachedStackInSlot = suppliedStack.copyWithCount(suppliedStack.getMaxStackSize());
   }

   public CreativeCrateMountedStorage(ItemStack suppliedStack) {
      this((MountedItemStorageType<?>)AllMountedStorageTypes.CREATIVE_CRATE.get(), suppliedStack);
   }

   @Override
   public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
   }

   public int getSlots() {
      return 2;
   }

   @NotNull
   public ItemStack getStackInSlot(int slot) {
      return slot == 0 ? this.cachedStackInSlot : ItemStack.EMPTY;
   }

   public void setStackInSlot(int slot, @NotNull ItemStack stack) {
   }

   @NotNull
   public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
      return ItemStack.EMPTY;
   }

   @NotNull
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (slot == 0 && !this.suppliedStack.isEmpty()) {
         int count = Math.min(amount, this.suppliedStack.getMaxStackSize());
         return this.suppliedStack.copyWithCount(count);
      } else {
         return ItemStack.EMPTY;
      }
   }

   public int getSlotLimit(int slot) {
      return 64;
   }

   public boolean isItemValid(int slot, @NotNull ItemStack stack) {
      return true;
   }
}
