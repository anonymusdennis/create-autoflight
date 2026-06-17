package net.createmod.ponder.api.element;

import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

public interface PonderSceneElement extends PonderElement {
   void renderFirst(PonderLevel var1, MultiBufferSource var2, GuiGraphics var3, float var4);

   void renderLayer(PonderLevel var1, MultiBufferSource var2, RenderType var3, GuiGraphics var4, float var5);

   void renderLast(PonderLevel var1, MultiBufferSource var2, GuiGraphics var3, float var4);
}
