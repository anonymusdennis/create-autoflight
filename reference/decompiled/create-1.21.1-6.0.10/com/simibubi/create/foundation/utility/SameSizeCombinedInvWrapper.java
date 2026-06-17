package com.simibubi.create.foundation.utility;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

public class SameSizeCombinedInvWrapper extends CombinedInvWrapper {
   private final int numSlotsPerInv;
   private final int numCombinedSlots;

   private SameSizeCombinedInvWrapper(int numSlotsPerInv, IItemHandlerModifiable... itemHandler) {
      super(itemHandler);
      this.numSlotsPerInv = numSlotsPerInv;
      this.numCombinedSlots = numSlotsPerInv * itemHandler.length;
   }

   public static CombinedInvWrapper create(IItemHandlerModifiable... itemHandler) {
      if (itemHandler.length == 0) {
         return new CombinedInvWrapper(itemHandler);
      } else {
         int firstInvNumSlots = itemHandler[0].getSlots();

         for (int i = 1; i < itemHandler.length; i++) {
            if (firstInvNumSlots != itemHandler[i].getSlots()) {
               return new CombinedInvWrapper(itemHandler);
            }
         }

         return new SameSizeCombinedInvWrapper(firstInvNumSlots, itemHandler);
      }
   }

   protected int getIndexForSlot(int slot) {
      return slot >= 0 && slot < this.numCombinedSlots ? slot / this.numSlotsPerInv : -1;
   }
}
