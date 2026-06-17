package net.createmod.ponder.foundation.content;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class DebugPonderPlugin implements PonderPlugin {
   @Override
   public String getModId() {
      return "ponder";
   }

   @Override
   public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
      DebugScenes.registerAll(helper);
   }
}
