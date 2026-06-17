package net.createmod.ponder.foundation.content;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;

public class BasePonderPlugin implements PonderPlugin {
   @Override
   public String getModId() {
      return "ponder";
   }

   @Override
   public void registerSharedText(SharedTextRegistrationHelper helper) {
      helper.registerSharedText("sneak_and", "Sneak +");
      helper.registerSharedText("ctrl_and", "Ctrl +");
   }
}
