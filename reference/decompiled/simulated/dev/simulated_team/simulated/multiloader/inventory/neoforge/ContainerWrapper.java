package dev.simulated_team.simulated.multiloader.inventory.neoforge;

import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class ContainerWrapper<T extends AbstractContainer> implements IItemHandlerModifiable {
   private final T container;

   public ContainerWrapper(T container) {
      this.container = container;
   }

   @NotNull
   public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
      return this.container.insertSlot(stack, slot, simulate);
   }

   @NotNull
   public ItemStack extractItem(int slot, int maxSize, boolean simulate) {
      return this.container.extractSlot(slot, maxSize, simulate);
   }

   public void setStackInSlot(int i, @NotNull ItemStack arg) {
      this.container.setItem(i, arg);
   }

   public int getSlots() {
      return this.container.getContainerSize();
   }

   @NotNull
   public ItemStack getStackInSlot(int i) {
      return this.container.getItem(i);
   }

   public int getSlotLimit(int i) {
      return this.container.getMaxStackSize();
   }

   public boolean isItemValid(int i, @NotNull ItemStack arg) {
      return true;
   }
}
