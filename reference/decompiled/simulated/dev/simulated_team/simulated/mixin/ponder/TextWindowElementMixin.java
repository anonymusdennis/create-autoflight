package dev.simulated_team.simulated.mixin.ponder;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.mixin_interface.ponder.TextWindowElementExtension;
import net.createmod.ponder.foundation.element.TextWindowElement;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({TextWindowElement.class})
public class TextWindowElementMixin implements TextWindowElementExtension {
   @Unique
   boolean simulated$shouldHidePointer = false;

   @Override
   public void simulated$hidePointer() {
      this.simulated$shouldHidePointer = true;
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIIII)V",
         ordinal = 0
      )}
   )
   public void simulated$removePointer(GuiGraphics instance, int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo, Operation<Void> original) {
      if (!this.simulated$shouldHidePointer) {
         original.call(new Object[]{instance, x1, y1, x2, y2, z, colorFrom, colorTo});
      }
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIIII)V",
         ordinal = 1
      )}
   )
   public void simulated$removePointerTwoTheSqueakuel(
      GuiGraphics instance, int x1, int y1, int x2, int y2, int z, int colorFrom, int colorTo, Operation<Void> original
   ) {
      if (!this.simulated$shouldHidePointer) {
         original.call(new Object[]{instance, x1, y1, x2, y2, z, colorFrom, colorTo});
      }
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Math;min(FF)F"
      )}
   )
   public float simulated$shiftItALittleToTheLeftIfThereIsntALine(float a, float b, Operation<Float> original) {
      return this.simulated$shouldHidePointer ? (Float)original.call(new Object[]{a, b - 50.0F}) : (Float)original.call(new Object[]{a, b});
   }
}
