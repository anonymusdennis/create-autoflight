package dev.simulated_team.simulated.index.ponder;

import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import dev.simulated_team.simulated.index.SimPonderScenes;
import dev.simulated_team.simulated.index.SimPonderTags;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SimPonderPlugin extends CreatePonderPlugin {
   public String getModId() {
      return "simulated";
   }

   public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
      SimPonderScenes.register(helper);
   }

   public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
      SimPonderTags.register(helper);
   }

   public void registerSharedText(@NotNull SharedTextRegistrationHelper helper) {
      helper.registerSharedText("property_tooltip_always", "Detailed property information can be previewed in the item tooltip");
      helper.registerSharedText("property_tooltip_shift", "Detailed property information can be previewed in the item tooltip while pressing Shift");
      helper.registerSharedText("property_tooltip_goggles", "Detailed property information can be previewed in the item tooltip while wearing Goggles");
      helper.registerSharedText(
         "property_tooltip_shift_goggles", "Detailed property information can be previewed in the item tooltip while wearing Goggles and pressing Shift"
      );
      helper.registerSharedText("property_tooltip_never", "However, you can't preview these properties because you disabled the tooltip...");
      helper.registerSharedText("property_tooltip_how", "But the config was invalid...");
      helper.registerSharedText("rpm16", "16 RPM");
   }

   public void onPonderLevelRestore(PonderLevel ponderLevel) {
   }

   public void indexExclusions(IndexExclusionHelper helper) {
   }
}
