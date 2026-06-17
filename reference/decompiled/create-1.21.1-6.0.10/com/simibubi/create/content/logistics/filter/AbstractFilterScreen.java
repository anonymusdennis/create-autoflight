package com.simibubi.create.content.logistics.filter;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.item.TooltipHelper;
import java.util.Collections;
import java.util.List;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.GuiGameElement.GuiRenderBuilder;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractFilterScreen<F extends AbstractFilterMenu> extends AbstractSimiContainerScreen<F> {
   protected AllGuiTextures background;
   private List<Rect2i> extraAreas = Collections.emptyList();
   private IconButton resetButton;
   private IconButton confirmButton;

   protected AbstractFilterScreen(F menu, Inventory inv, Component title, AllGuiTextures background) {
      super(menu, inv, title);
      this.background = background;
   }

   @Override
   protected void init() {
      this.setWindowSize(
         Math.max(this.background.getWidth(), AllGuiTextures.PLAYER_INVENTORY.getWidth()),
         this.background.getHeight() + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight()
      );
      super.init();
      int x = this.leftPos;
      int y = this.topPos;
      this.resetButton = new IconButton(x + this.background.getWidth() - 62, y + this.background.getHeight() - 24, AllIcons.I_TRASH);
      this.resetButton.withCallback(() -> {
         ((AbstractFilterMenu)this.menu).clearContents();
         this.contentsCleared();
         ((AbstractFilterMenu)this.menu).sendClearPacket();
      });
      this.confirmButton = new IconButton(x + this.background.getWidth() - 33, y + this.background.getHeight() - 24, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.minecraft.player.closeContainer());
      this.addRenderableWidget(this.resetButton);
      this.addRenderableWidget(this.confirmButton);
      this.extraAreas = ImmutableList.of(new Rect2i(x + this.background.getWidth(), y + this.background.getHeight() - 40, 80, 48));
   }

   protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      int invX = this.getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.getWidth());
      int invY = this.topPos + this.background.getHeight() + 4;
      this.renderPlayerInventory(graphics, invX, invY);
      int x = this.leftPos;
      int y = this.topPos;
      this.background.render(graphics, x, y);
      graphics.drawString(this.font, this.title, x + (this.background.getWidth() - 8) / 2 - this.font.width(this.title) / 2, y + 4, this.getTitleColor(), false);
      ((GuiRenderBuilder)GuiGameElement.of(((AbstractFilterMenu)this.menu).contentHolder)
            .at((float)(x + this.background.getWidth() + 8), (float)(y + this.background.getHeight() - 52), -200.0F))
         .scale(4.0)
         .render(graphics);
   }

   protected int getTitleColor() {
      return 5841956;
   }

   @Override
   protected void containerTick() {
      if (!ItemStack.matches(((AbstractFilterMenu)this.menu).player.getMainHandItem(), ((AbstractFilterMenu)this.menu).contentHolder)) {
         ((AbstractFilterMenu)this.menu).player.closeContainer();
      }

      super.containerTick();
      this.handleTooltips();
      this.handleIndicators();
   }

   protected void handleTooltips() {
      List<IconButton> tooltipButtons = this.getTooltipButtons();

      for (IconButton button : tooltipButtons) {
         if (!button.getToolTip().isEmpty()) {
            button.setToolTip((Component)button.getToolTip().get(0));
            button.getToolTip().add(TooltipHelper.holdShift(Palette.YELLOW, hasShiftDown()));
         }
      }

      if (hasShiftDown()) {
         List<MutableComponent> tooltipDescriptions = this.getTooltipDescriptions();

         for (int i = 0; i < tooltipButtons.size(); i++) {
            this.fillToolTip(tooltipButtons.get(i), (Component)tooltipDescriptions.get(i));
         }
      }
   }

   public void handleIndicators() {
      for (IconButton button : this.getTooltipButtons()) {
         button.green = !this.isButtonEnabled(button);
      }
   }

   protected abstract boolean isButtonEnabled(IconButton var1);

   protected List<IconButton> getTooltipButtons() {
      return Collections.emptyList();
   }

   protected List<MutableComponent> getTooltipDescriptions() {
      return Collections.emptyList();
   }

   private void fillToolTip(IconButton button, Component tooltip) {
      if (button.isHoveredOrFocused()) {
         List<Component> tip = button.getToolTip();
         tip.addAll(TooltipHelper.cutTextComponent(tooltip, Palette.ALL_GRAY));
      }
   }

   protected void contentsCleared() {
   }

   protected void sendOptionUpdate(FilterScreenPacket.Option option) {
      CatnipServices.NETWORK.sendToServer(new FilterScreenPacket(option));
   }

   @Override
   public List<Rect2i> getExtraAreas() {
      return this.extraAreas;
   }
}
