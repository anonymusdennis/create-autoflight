package dev.simulated_team.simulated.mixin.hold_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import dev.simulated_team.simulated.index.SimClickInteractions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GoggleOverlayRenderer.class})
public class GoggleOverlayRendererMixin {
   @Shadow
   public static int hoverTicks;

   @Inject(
      method = {"renderOverlay"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;clamp(FFF)F",
         shift = Shift.BEFORE
      )},
      remap = false
   )
   private static void decrementRenderTicks(CallbackInfo ci) {
      if (SimClickInteractions.STEERING_WHEEL_MANAGER.isActive()) {
         hoverTicks = Mth.clamp(hoverTicks - 2, 0, 24);
      }
   }

   @WrapOperation(
      method = {"renderOverlay"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;clamp(FFF)F"
      )},
      remap = false
   )
   private static float fixPartialTicks(float value, float min, float max, Operation<Float> original, @Local(argsOnly = true) DeltaTracker deltaTracker) {
      return SimClickInteractions.STEERING_WHEEL_MANAGER.isActive()
         ? Mth.clamp((float)hoverTicks - deltaTracker.getGameTimeDeltaTicks(), 0.0F, 24.0F) / 24.0F
         : (Float)original.call(new Object[]{value, min, max});
   }

   @Inject(
      method = {"renderOverlay"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;clamp(FFF)F"
      )},
      remap = false,
      cancellable = true
   )
   private static void dontRenderTheText(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if ((float)hoverTicks - deltaTracker.getGameTimeDeltaTicks() <= 0.0F) {
         ci.cancel();
      }
   }
}
