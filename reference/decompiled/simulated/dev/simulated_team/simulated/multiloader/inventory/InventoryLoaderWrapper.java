package dev.simulated_team.simulated.multiloader.inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class InventoryLoaderWrapper implements AbstractContainer {
   public Consumer<Boolean> callback;

   public abstract ItemStack extractAny(int var1, boolean var2, boolean var3);

   @Override
   public void setItem(int slot, @NotNull ItemStack stack) {
   }

   @Override
   public void clearContent() {
   }

   @Override
   public void setChanged() {
   }

   @Override
   public CompoundTag write(Provider provider) {
      return new CompoundTag();
   }

   @Override
   public void read(Provider provider, CompoundTag nbt) {
   }

   @Override
   public boolean isEmpty() {
      return false;
   }

   @Override
   public List<ContainerSlot> getInventoryAsList() {
      return new ArrayList<>();
   }

   @Override
   public Set<ContainerSlot> getPopulatedSlots() {
      return new HashSet<>();
   }

   public void inventoryModificationCallback(Consumer<Boolean> callback) {
      this.callback = callback;
   }
}
