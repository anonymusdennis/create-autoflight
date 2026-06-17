package net.createmod.catnip.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

public interface StencilElement extends RenderElement {
   @Override
   default void render(GuiGraphics graphics) {
      graphics.pose().pushPose();
      this.transform(graphics);
      this.prepareStencil(graphics);
      this.renderStencil(graphics);
      this.prepareElement(graphics);
      this.renderElement(graphics);
      this.cleanUp(graphics);
      graphics.pose().popPose();
   }

   void renderStencil(GuiGraphics var1);

   void renderElement(GuiGraphics var1);

   default void transform(GuiGraphics graphics) {
      graphics.pose().translate(this.getX(), this.getY(), this.getZ());
   }

   default void prepareStencil(GuiGraphics graphics) {
      graphics.flush();
      GL11.glDisable(2960);
      RenderSystem.stencilMask(-1);
      RenderSystem.clear(1024, Minecraft.ON_OSX);
      GL11.glEnable(2960);
      RenderSystem.stencilOp(7681, 7680, 7680);
      RenderSystem.stencilMask(255);
      RenderSystem.stencilFunc(512, 1, 255);
   }

   default void prepareElement(GuiGraphics graphics) {
      GL11.glEnable(2960);
      RenderSystem.stencilOp(7680, 7680, 7680);
      RenderSystem.stencilFunc(514, 1, 255);
   }

   default void cleanUp(GuiGraphics graphics) {
      GL11.glDisable(2960);
      graphics.flush();
   }
}
