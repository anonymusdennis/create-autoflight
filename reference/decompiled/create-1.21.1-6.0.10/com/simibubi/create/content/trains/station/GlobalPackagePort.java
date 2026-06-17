package com.simibubi.create.content.trains.station;

import com.simibubi.create.Create;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

public class GlobalPackagePort {
   public String address = "";
   public ItemStackHandler offlineBuffer = new ItemStackHandler(18);
   public boolean primed = false;
   private boolean restoring = false;

   public void restoreOfflineBuffer(IItemHandlerModifiable inventory) {
      if (this.primed) {
         this.restoring = true;

         for (int slot = 0; slot < this.offlineBuffer.getSlots(); slot++) {
            inventory.setStackInSlot(slot, this.offlineBuffer.getStackInSlot(slot));
         }

         this.restoring = false;
         this.primed = false;
      }
   }

   public void saveOfflineBuffer(IItemHandlerModifiable inventory) {
      if (!this.restoring) {
         for (int slot = 0; slot < inventory.getSlots(); slot++) {
            this.offlineBuffer.setStackInSlot(slot, inventory.getStackInSlot(slot));
         }

         Create.RAILWAYS.markTracksDirty();
      }
   }
}
