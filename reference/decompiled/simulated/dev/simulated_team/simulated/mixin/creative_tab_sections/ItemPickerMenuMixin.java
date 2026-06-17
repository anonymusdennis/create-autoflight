package dev.simulated_team.simulated.mixin.creative_tab_sections;

import dev.simulated_team.simulated.registrate.simulated_tab.SimulatedCreativeTab;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.ItemPickerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ItemPickerMenu.class})
public abstract class ItemPickerMenuMixin {
   @Shadow
   protected abstract int getRowIndexForScroll(float var1);

   @Inject(
      method = {"scrollTo"},
      at = {@At("HEAD")}
   )
   private void simulated$scrollTo(float f, CallbackInfo ci) {
      SimulatedCreativeTab.CURRENT_ROW = this.getRowIndexForScroll(f);
   }
}
