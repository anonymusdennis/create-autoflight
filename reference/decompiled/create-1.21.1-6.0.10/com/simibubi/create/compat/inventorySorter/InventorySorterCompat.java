package com.simibubi.create.compat.inventorySorter;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterMenu;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

public class InventorySorterCompat {
   public static final String SLOT_BLACKLIST = "slotblacklist";

   public static void init(IEventBus bus) {
      bus.addListener(InventorySorterCompat::sendImc);
   }

   private static void sendImc(InterModEnqueueEvent event) {
      InterModComms.sendTo(Mods.INVENTORYSORTER.id(), "slotblacklist", RedstoneRequesterMenu.SorterProofSlot.class::getName);
   }
}
