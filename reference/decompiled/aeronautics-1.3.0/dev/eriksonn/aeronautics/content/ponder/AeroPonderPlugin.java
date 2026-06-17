package dev.eriksonn.aeronautics.content.ponder;

import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import dev.eriksonn.aeronautics.index.AeroPonderScenes;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public class AeroPonderPlugin extends CreatePonderPlugin {
   public String getModId() {
      return "aeronautics";
   }

   public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
      AeroPonderScenes.register(helper);
   }

   public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
      AeroPonderTags.register(helper);
   }

   public void registerSharedText(SharedTextRegistrationHelper helper) {
   }

   public void onPonderLevelRestore(PonderLevel ponderLevel) {
   }

   public void indexExclusions(IndexExclusionHelper helper) {
   }
}
