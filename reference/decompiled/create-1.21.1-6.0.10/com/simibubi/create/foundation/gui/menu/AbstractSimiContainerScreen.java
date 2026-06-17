package com.simibubi.create.foundation.gui.menu;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.TickableGuiEventListener;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
public abstract class AbstractSimiContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
   protected int windowXOffset;
   protected int windowYOffset;

   public AbstractSimiContainerScreen(T container, Inventory inv, Component title) {
      super(container, inv, title);
   }

   protected void setWindowSize(int width, int height) {
      this.imageWidth = width;
      this.imageHeight = height;
   }

   protected void setWindowOffset(int xOffset, int yOffset) {
      this.windowXOffset = xOffset;
      this.windowYOffset = yOffset;
   }

   protected void init() {
      super.init();
      this.leftPos = this.leftPos + this.windowXOffset;
      this.topPos = this.topPos + this.windowYOffset;
   }

   protected void containerTick() {
      for (GuiEventListener listener : this.children()) {
         if (listener instanceof TickableGuiEventListener tickable) {
            tickable.tick();
         }
      }
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
      partialTicks = AnimationTickHolder.getPartialTicksUI();
      super.render(graphics, mouseX, mouseY, partialTicks);
      this.renderForeground(graphics, mouseX, mouseY, partialTicks);
   }

   protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
   }

   protected void renderForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.renderTooltip(graphics, mouseX, mouseY);

      for (Renderable widget : this.renderables) {
         if (widget instanceof AbstractSimiWidget) {
            AbstractSimiWidget simiWidget = (AbstractSimiWidget)widget;
            if (simiWidget.isMouseOver((double)mouseX, (double)mouseY)) {
               List<Component> tooltip = simiWidget.getToolTip();
               if (!tooltip.isEmpty()) {
                  int ttx = simiWidget.lockedTooltipX == -1 ? mouseX : simiWidget.lockedTooltipX + simiWidget.getX();
                  int tty = simiWidget.lockedTooltipY == -1 ? mouseY : simiWidget.lockedTooltipY + simiWidget.getY();
                  graphics.renderComponentTooltip(this.font, tooltip, ttx, tty);
               }
            }
         }
      }
   }

   public int getLeftOfCentered(int textureWidth) {
      return this.leftPos - this.windowXOffset + (this.imageWidth - textureWidth) / 2;
   }

   public void renderPlayerInventory(GuiGraphics graphics, int x, int y) {
      AllGuiTextures.PLAYER_INVENTORY.render(graphics, x, y);
      graphics.drawString(this.font, this.playerInventoryTitle, x + 8, y + 6, 4210752, false);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      return this.getFocused() instanceof EditBox && pKeyCode != 256
         ? this.getFocused().keyPressed(pKeyCode, pScanCode, pModifiers)
         : super.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (this.getFocused() != null && !this.getFocused().isMouseOver(pMouseX, pMouseY)) {
         this.setFocused(null);
      }

      return super.mouseClicked(pMouseX, pMouseY, pButton);
   }

   public GuiEventListener getFocused() {
      GuiEventListener focused = super.getFocused();
      if (focused instanceof AbstractWidget && !((AbstractWidget)focused).isFocused()) {
         focused = null;
      }

      this.setFocused(focused);
      return focused;
   }

   public List<Rect2i> getExtraAreas() {
      return Collections.emptyList();
   }

   @Deprecated
   protected void debugWindowArea(GuiGraphics graphics) {
      graphics.fill(this.leftPos + this.imageWidth, this.topPos + this.imageHeight, this.leftPos, this.topPos, -741092397);
   }

   @Deprecated
   protected void debugExtraAreas(GuiGraphics graphics) {
      for (Rect2i area : this.getExtraAreas()) {
         graphics.fill(area.getX() + area.getWidth(), area.getY() + area.getHeight(), area.getX(), area.getY(), -741092397);
      }
   }

   protected void playUiSound(SoundEvent sound, float volume, float pitch) {
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume * 0.25F));
   }
}
