package dev.simulated_team.simulated.mixin.new_ponder;

import dev.simulated_team.simulated.ponder.new_ponder_tooltip.NewPonderTooltipManager;
import java.util.List;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PonderUI.class})
public class PonderUIMixin {
   @Shadow
   @Final
   private List<PonderScene> scenes;
   @Shadow
   private int index;

   @Inject(
      method = {"scroll"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/createmod/ponder/foundation/PonderScene;begin()V"
      )}
   )
   private void simulated$begin(boolean forward, CallbackInfoReturnable<Boolean> cir) {
      NewPonderTooltipManager.setSceneWatched(this.scenes.get(this.index).getId());
   }
}
