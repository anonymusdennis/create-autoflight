package com.simibubi.create.api.contraption.storage.item;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

public class MountedItemStorageWrapper extends CombinedInvWrapper {
   public final ImmutableMap<BlockPos, MountedItemStorage> storages;
   private final int[] slotToStorage;
   private final int[] slotOffsets;

   public MountedItemStorageWrapper(ImmutableMap<BlockPos, MountedItemStorage> storages) {
      super((IItemHandlerModifiable[])storages.values().toArray(IItemHandlerModifiable[]::new));
      this.storages = storages;
      int totalSlots = this.getSlots();
      this.slotToStorage = new int[totalSlots];
      this.slotOffsets = new int[this.itemHandler.length];
      int currentSlot = 0;

      for (int storageIdx = 0; storageIdx < this.itemHandler.length; storageIdx++) {
         this.slotOffsets[storageIdx] = currentSlot;
         int slotsInStorage = this.itemHandler[storageIdx].getSlots();

         for (int i = 0; i < slotsInStorage; i++) {
            this.slotToStorage[currentSlot + i] = storageIdx;
         }

         currentSlot += slotsInStorage;
      }
   }

   protected int getIndexForSlot(int slot) {
      return slot >= 0 && slot < this.slotToStorage.length ? this.slotToStorage[slot] : -1;
   }

   protected int getSlotFromIndex(int slot, int index) {
      return slot - this.slotOffsets[index];
   }
}
