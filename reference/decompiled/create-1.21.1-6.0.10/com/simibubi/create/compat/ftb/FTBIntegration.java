package com.simibubi.create.compat.ftb;

import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent.Closing;
import net.neoforged.neoforge.client.event.ScreenEvent.Opening;

public class FTBIntegration {
   private static boolean buttonStatePreviously;

   public static void init(IEventBus modEventBus, IEventBus forgeEventBus) {
   }

   private static void removeGUIClutterOpen(Opening event) {
      if (!isCreate(event.getCurrentScreen())) {
         if (isCreate(event.getNewScreen())) {
            ;
         }
      }
   }

   private static void removeGUIClutterClose(Closing event) {
      if (isCreate(event.getScreen())) {
         ;
      }
   }

   private static boolean isCreate(Screen screen) {
      return screen instanceof AbstractSimiContainerScreen || screen instanceof AbstractSimiScreen;
   }
}
