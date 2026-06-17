package dev.simulated_team.simulated.multiloader.inventory;

import java.util.List;
import java.util.Set;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface AbstractContainer extends NBTSerializable, Container {
   default int commonInsert(ItemInfoWrapper info, ContainerSlot slot, int insertAmount, boolean simulate) {
      return slot.insertStack(info, insertAmount, simulate);
   }

   default int commonExtract(ItemInfoWrapper info, ContainerSlot slot, int extractAmount, boolean simulate) {
      return slot.extractStack(info, extractAmount, simulate);
   }

   int insertGeneral(ItemInfoWrapper var1, int var2, boolean var3);

   ItemStack insertSlot(ItemStack var1, int var2, boolean var3);

   int extractGeneral(ItemInfoWrapper var1, int var2, boolean var3);

   ItemStack extractSlot(int var1, int var2, boolean var3);

   default boolean canInsertItem(ItemInfoWrapper info, ContainerSlot slot) {
      return true;
   }

   default boolean canExtractFromSlot(ContainerSlot slot) {
      return true;
   }

   default void populateFields(ContainerSlot containerSlot) {
   }

   default void onStackItemChange(ContainerSlot slot, ItemStack oldSlotStack, ItemStack newSlotStack) {
   }

   @NotNull
   default ItemStack removeItem(int slot, int amount) {
      ItemStack item = this.getItem(slot);
      return item.split(amount);
   }

   @NotNull
   default ItemStack removeItemNoUpdate(int slot) {
      ItemStack item = this.getItem(slot);
      this.setItem(slot, ItemStack.EMPTY);
      return item;
   }

   default boolean stillValid(@NotNull Player player) {
      return true;
   }

   int getContainerSize();

   int getMaxStackSize();

   boolean isEmpty();

   @NotNull
   ItemStack getItem(int var1);

   void setItem(int var1, @NotNull ItemStack var2);

   List<ContainerSlot> getInventoryAsList();

   Set<ContainerSlot> getPopulatedSlots();

   void clearContent();

   void setChanged();
}
