package dev.simulated_team.simulated.mixin.new_ponder;

import dev.simulated_team.simulated.ponder.new_ponder_tooltip.NewPonderTooltipManager;
import java.util.List;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PonderSceneRegistry.class})
public class PonderSceneRegistryMixin {
   @Inject(
      method = {"compile(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/List;"},
      at = {@At("RETURN")}
   )
   private void simulated$compile(ResourceLocation id, CallbackInfoReturnable<List<PonderScene>> cir) {
      NewPonderTooltipManager.setSceneWatched(((PonderScene)((List)cir.getReturnValue()).getFirst()).getId());
   }
}
