package net.createmod.ponder.api.element;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.GuiGraphics;

public interface PonderOverlayElement extends PonderElement {
   void render(PonderScene var1, PonderUI var2, GuiGraphics var3, float var4);
}
