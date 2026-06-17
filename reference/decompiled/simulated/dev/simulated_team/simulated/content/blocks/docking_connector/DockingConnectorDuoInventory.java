package dev.simulated_team.simulated.content.blocks.docking_connector;

import dev.simulated_team.simulated.multiloader.inventory.AbstractContainer;
import dev.simulated_team.simulated.multiloader.inventory.ContainerSlot;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import java.util.List;
import java.util.Set;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public class DockingConnectorDuoInventory implements AbstractContainer {
   private final DockingConnectorSoloInventory ourInventory;
   private final DockingConnectorSoloInventory theirInventory;

   public DockingConnectorDuoInventory(DockingConnectorBlockEntity ourConnector, DockingConnectorBlockEntity theirConnector) {
      this.ourInventory = ourConnector.inventory;
      this.theirInventory = theirConnector.inventory;
   }

   @Override
   public int insertGeneral(ItemInfoWrapper info, int amountToInsert, boolean simulate) {
      return this.theirInventory.insertGeneral(info, amountToInsert, simulate);
   }

   @Override
   public ItemStack insertSlot(ItemStack stack, int slot, boolean simulate) {
      return slot == 0 ? this.theirInventory.insertSlot(stack, 0, simulate) : stack;
   }

   @Override
   public int extractGeneral(ItemInfoWrapper info, int amountToExtract, boolean simulate) {
      return this.ourInventory.extractGeneral(info, amountToExtract, simulate);
   }

   @Override
   public ItemStack extractSlot(int index, int amountToExtract, boolean simulate) {
      return index == 1 ? this.ourInventory.extractSlot(0, amountToExtract, simulate) : ItemStack.EMPTY;
   }

   @Override
   public int getContainerSize() {
      return 2;
   }

   @Override
   public int getMaxStackSize() {
      return 64;
   }

   @NotNull
   @Override
   public ItemStack getItem(int slot) {
      return switch (slot) {
         case 0 -> this.theirInventory.getItem(0);
         case 1 -> this.ourInventory.getItem(0);
         default -> ItemStack.EMPTY;
      };
   }

   @Override
   public void setItem(int slot, @NotNull ItemStack stack) {
      switch (slot) {
         case 0:
            this.theirInventory.setItem(0, stack);
            break;
         case 1:
            this.ourInventory.setItem(0, stack);
      }
   }

   @Override
   public void clearContent() {
   }

   @Override
   public void setChanged() {
      this.ourInventory.setChanged();
      this.theirInventory.setChanged();
   }

   @Override
   public CompoundTag write(Provider provider) {
      throw new NotImplementedException();
   }

   @Override
   public void read(Provider provider, CompoundTag nbt) {
      throw new NotImplementedException();
   }

   @Override
   public boolean isEmpty() {
      return this.ourInventory.isEmpty() && this.theirInventory.isEmpty();
   }

   @Override
   public List<ContainerSlot> getInventoryAsList() {
      return List.of(this.ourInventory.slot, this.theirInventory.slot);
   }

   @Override
   public Set<ContainerSlot> getPopulatedSlots() {
      return this.theirInventory.isEmpty() ? Set.of() : Set.of(this.ourInventory.slot);
   }
}
