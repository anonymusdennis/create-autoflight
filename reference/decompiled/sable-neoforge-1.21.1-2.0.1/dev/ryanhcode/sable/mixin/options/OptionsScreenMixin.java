package dev.ryanhcode.sable.mixin.options;

import dev.ryanhcode.sable.config.SubLevelSettingsScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({OptionsScreen.class})
public abstract class OptionsScreenMixin extends Screen {
   @Shadow
   @Final
   private Options options;

   protected OptionsScreenMixin(Component component) {
      super(component);
   }

   @Inject(
      method = {"createOnlineButton"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void sable$createSableButton(CallbackInfoReturnable<LayoutElement> cir) {
      if (this.minecraft.level != null && this.minecraft.hasSingleplayerServer()) {
         LinearLayout layout = LinearLayout.vertical();
         Button sableButton = Button.builder(
               SubLevelSettingsScreen.TITLE, event -> this.minecraft.setScreen(new SubLevelSettingsScreen(this, this.options, SubLevelSettingsScreen.TITLE))
            )
            .pos(0, 30)
            .size(150, 20)
            .build();
         layout.addChild((LayoutElement)cir.getReturnValue());
         layout.spacing(5);
         layout.addChild(sableButton);
         cir.setReturnValue(layout);
      }
   }
}
