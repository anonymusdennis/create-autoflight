package net.createmod.catnip.gui.element;

import net.minecraft.client.gui.GuiGraphics;

public abstract class AbstractRenderElement implements RenderElement {
   public static RenderElement EMPTY = new AbstractRenderElement() {
      @Override
      public void render(GuiGraphics graphics) {
      }
   };
   protected int width = 16;
   protected int height = 16;
   protected float x = 0.0F;
   protected float y = 0.0F;
   protected float z = 0.0F;
   protected float alpha = 1.0F;

   @Override
   public <T extends RenderElement> T at(float x, float y) {
      this.x = x;
      this.y = y;
      return (T)this;
   }

   @Override
   public <T extends RenderElement> T at(float x, float y, float z) {
      this.x = x;
      this.y = y;
      this.z = z;
      return (T)this;
   }

   @Override
   public <T extends RenderElement> T withBounds(int width, int height) {
      this.width = width;
      this.height = height;
      return (T)this;
   }

   @Override
   public <T extends RenderElement> T withAlpha(float alpha) {
      this.alpha = alpha;
      return (T)this;
   }

   @Override
   public int getWidth() {
      return this.width;
   }

   @Override
   public int getHeight() {
      return this.height;
   }

   @Override
   public float getX() {
      return this.x;
   }

   @Override
   public float getY() {
      return this.y;
   }

   @Override
   public float getZ() {
      return this.z;
   }

   public static class SimpleRenderElement extends AbstractRenderElement {
      private final ScreenElement renderable;

      public SimpleRenderElement(ScreenElement renderable) {
         this.renderable = renderable;
      }

      @Override
      public void render(GuiGraphics graphics) {
         this.renderable.render(graphics, (int)this.x, (int)this.y);
      }
   }
}
