package net.createmod.ponder.api.element;

import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.world.item.ItemStack;

public interface InputElementBuilder {
   InputElementBuilder withItem(ItemStack var1);

   InputElementBuilder leftClick();

   InputElementBuilder rightClick();

   InputElementBuilder scroll();

   InputElementBuilder showing(ScreenElement var1);

   InputElementBuilder whileSneaking();

   InputElementBuilder whileCTRL();
}
