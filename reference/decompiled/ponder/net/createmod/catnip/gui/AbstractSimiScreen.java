package net.createmod.catnip.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.List;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.mixin.client.accessor.ScreenAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class AbstractSimiScreen extends Screen {
   protected static final Color BACKGROUND_COLOR = new Color(1343229968, true);
   protected int windowWidth;
   protected int windowHeight;
   protected int windowXOffset;
   protected int windowYOffset;
   protected int guiLeft;
   protected int guiTop;

   protected AbstractSimiScreen(Component title) {
      super(title);
   }

   protected AbstractSimiScreen() {
      this(CommonComponents.EMPTY);
   }

   protected void setWindowSize(int width, int height) {
      this.windowWidth = width;
      this.windowHeight = height;
   }

   protected void setWindowOffset(int xOffset, int yOffset) {
      this.windowXOffset = xOffset;
      this.windowYOffset = yOffset;
   }

   protected void init() {
      this.guiLeft = (this.width - this.windowWidth) / 2;
      this.guiTop = (this.height - this.windowHeight) / 2;
      this.guiLeft = this.guiLeft + this.windowXOffset;
      this.guiTop = this.guiTop + this.windowYOffset;
   }

   public void tick() {
      for (GuiEventListener listener : this.children()) {
         if (listener instanceof TickableGuiEventListener tickable) {
            tickable.tick();
         }
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(W... widgets) {
      for (W widget : widgets) {
         this.addRenderableWidget(widget);
      }
   }

   protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
      for (W widget : widgets) {
         this.addRenderableWidget(widget);
      }
   }

   protected void removeWidgets(GuiEventListener... widgets) {
      for (GuiEventListener widget : widgets) {
         this.removeWidget(widget);
      }
   }

   protected void removeWidgets(Collection<? extends GuiEventListener> widgets) {
      for (GuiEventListener widget : widgets) {
         this.removeWidget(widget);
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      partialTicks = NavigatableSimiScreen.currentlyRenderingPreviousScreen ? 0.0F : AnimationTickHolder.getPartialTicksUI();
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      this.prepareFrame();
      this.renderMenuBackground(graphics);
      this.renderWindowBackground(graphics, mouseX, mouseY, partialTicks);
      this.renderWindow(graphics, mouseX, mouseY, partialTicks);

      for (Renderable renderable : this.getRenderables()) {
         renderable.render(graphics, mouseX, mouseY, partialTicks);
      }

      this.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
      this.endFrame();
      poseStack.popPose();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      boolean keyPressed = super.keyPressed(keyCode, scanCode, modifiers);
      if (!keyPressed && this.getFocused() == null) {
         if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
         } else {
            boolean consumed = false;

            for (GuiEventListener widget : this.children()) {
               if (widget instanceof AbstractSimiWidget) {
                  AbstractSimiWidget simiWidget = (AbstractSimiWidget)widget;
                  if (simiWidget.keyPressed(keyCode, scanCode, modifiers)) {
                     consumed = true;
                  }
               }
            }

            return consumed;
         }
      } else {
         return keyPressed;
      }
   }

   protected void prepareFrame() {
   }

   protected void renderWindowBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.renderBackground(graphics, mouseX, mouseY, partialTicks);
   }

   protected abstract void renderWindow(GuiGraphics var1, int var2, int var3, float var4);

   protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
   }

   protected void endFrame() {
   }

   @Deprecated
   protected void debugWindowArea(GuiGraphics graphics) {
      graphics.fill(this.guiLeft + this.windowWidth, this.guiTop + this.windowHeight, this.guiLeft, this.guiTop, -741092397);
   }

   protected List<Renderable> getRenderables() {
      return ((ScreenAccessor)this).catnip$getRenderables();
   }

   public GuiEventListener getFocused() {
      GuiEventListener focused = super.getFocused();
      if (focused instanceof AbstractWidget && !focused.isFocused()) {
         focused = null;
      }

      this.setFocused(focused);
      return focused;
   }
}
