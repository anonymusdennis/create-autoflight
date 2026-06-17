package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.events.SimulatedCommonClientEvents;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MouseHandlerMixin {
   @Inject(
      method = {"turnPlayer"},
      cancellable = true,
      at = {@At(
         value = "INVOKE",
         shift = Shift.BEFORE,
         target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
      )}
   )
   private void simulated$turnPlayer(double d, CallbackInfo ci, @Local(ordinal = 4) double j, @Local(ordinal = 5) double k, @Local(ordinal = 0) int l) {
      if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
         InteractCallback.Result status = SimulatedCommonClientEvents.onMouseMove(j, k * (double)l);
         if (status.cancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"onPress"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;",
         ordinal = 0
      )},
      cancellable = true
   )
   private void simulated$preOnPress(
      long windowPointer,
      int button,
      int action,
      int modifiers,
      CallbackInfo ci,
      @Local(ordinal = 1,argsOnly = true) int i,
      @Local(argsOnly = true,ordinal = 0) long l
   ) {
      if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
         InteractCallback.Result status = SimulatedCommonClientEvents.onBeforeMouseInput(InteractCallback.Input.mouse(button), modifiers, action);
         if (status.cancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"onScroll"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;",
         ordinal = 0
      )},
      cancellable = true
   )
   private void simulated$preOnScroll(long l, double d, double e, CallbackInfo ci, @Local(ordinal = 3) double deltaX, @Local(ordinal = 4) double deltaY) {
      if (SimDistUtil.getClientPlayer() != null && !SimDistUtil.getClientPlayer().isSpectator()) {
         InteractCallback.Result status = SimulatedCommonClientEvents.onMouseScroll(deltaX, deltaY);
         if (status.cancelled()) {
            ci.cancel();
         }
      }
   }
}
