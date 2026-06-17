package dev.simulated_team.simulated.mixin.conditional_display_target;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkScreen;
import com.simibubi.create.foundation.gui.widget.Label;
import dev.simulated_team.simulated.api.ConditionalDisplayTarget;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({DisplayLinkScreen.class})
public class DisplayLinkScreenMixin {
   @Shadow
   private DisplayLinkBlockEntity blockEntity;
   @Shadow
   private Label targetLineLabel;

   @WrapOperation(
      method = {"initGathererOptions"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/api/behaviour/display/DisplayTarget;getLineOptionText(I)Lnet/minecraft/network/chat/Component;"
      )}
   )
   private Component simulated$displayConditionalError(
      DisplayTarget instance, int line, Operation<Component> original, @Local(name = {"level"}) ClientLevel level
   ) {
      DisplayLinkContext context = new DisplayLinkContext(level, this.blockEntity);
      if (instance instanceof ConditionalDisplayTarget cdt && !cdt.allowsWriting(context)) {
         this.targetLineLabel.colored(ChatFormatting.GRAY.getColor());
         return cdt.getErrorMessage(context);
      }

      return (Component)original.call(new Object[]{instance, line});
   }
}
