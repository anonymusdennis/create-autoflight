package com.simibubi.create.content.equipment.blueprint;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.filter.FilterScreenPacket;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.GuiGameElement.GuiRenderBuilder;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BlueprintScreen extends AbstractSimiContainerScreen<BlueprintMenu> {
   protected AllGuiTextures background;
   private List<Rect2i> extraAreas = Collections.emptyList();
   private IconButton resetButton;
   private IconButton confirmButton;

   public BlueprintScreen(BlueprintMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.background = AllGuiTextures.BLUEPRINT;
   }

   @Override
   protected void init() {
      this.setWindowSize(this.background.getWidth(), this.background.getHeight() + 4 + AllGuiTextures.PLAYER_INVENTORY.getHeight());
      this.setWindowOffset(1, 0);
      super.init();
      int x = this.leftPos;
      int y = this.topPos;
      this.resetButton = new IconButton(x + this.background.getWidth() - 62, y + this.background.getHeight() - 24, AllIcons.I_TRASH);
      this.resetButton.withCallback(() -> {
         ((BlueprintMenu)this.menu).clearContents();
         this.contentsCleared();
         ((BlueprintMenu)this.menu).sendClearPacket();
      });
      this.confirmButton = new IconButton(x + this.background.getWidth() - 33, y + this.background.getHeight() - 24, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.minecraft.player.closeContainer());
      this.addRenderableWidget(this.resetButton);
      this.addRenderableWidget(this.confirmButton);
      this.extraAreas = ImmutableList.of(new Rect2i(x + this.background.getWidth(), y + this.background.getHeight() - 36, 56, 44));
   }

   protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
      int invX = this.getLeftOfCentered(AllGuiTextures.PLAYER_INVENTORY.getWidth());
      int invY = this.topPos + this.background.getHeight() + 4;
      this.renderPlayerInventory(graphics, invX, invY);
      int x = this.leftPos;
      int y = this.topPos;
      this.background.render(graphics, x, y);
      graphics.drawString(this.font, this.title, x + 15, y + 4, 16777215, false);
      ((GuiRenderBuilder)GuiGameElement.of(AllPartialModels.CRAFTING_BLUEPRINT_1x1)
            .at((float)(x + this.background.getWidth() + 20), (float)(y + this.background.getHeight() - 32), 0.0F))
         .rotate(45.0, -45.0, 22.5)
         .scale(40.0)
         .render(graphics);
   }

   protected void renderTooltip(GuiGraphics graphics, int x, int y) {
      if (((BlueprintMenu)this.menu).getCarried().isEmpty()
         && this.hoveredSlot != null
         && this.hoveredSlot.container != ((BlueprintMenu)this.menu).playerInventory) {
         List<Component> list = new LinkedList<>();
         if (this.hoveredSlot.hasItem()) {
            list = this.getTooltipFromContainerItem(this.hoveredSlot.getItem());
         }

         graphics.renderComponentTooltip(this.font, this.addToTooltip(list, this.hoveredSlot.getSlotIndex(), true), x, y);
      } else {
         super.renderTooltip(graphics, x, y);
      }
   }

   private List<Component> addToTooltip(List<Component> list, int slot, boolean isEmptySlot) {
      if (slot >= 0 && slot <= 10) {
         if (slot < 9) {
            list.add(CreateLang.translateDirect("crafting_blueprint.crafting_slot").withStyle(ChatFormatting.GOLD));
            if (isEmptySlot) {
               list.add(CreateLang.translateDirect("crafting_blueprint.filter_items_viable").withStyle(ChatFormatting.GRAY));
            }
         } else if (slot == 9) {
            list.add(CreateLang.translateDirect("crafting_blueprint.display_slot").withStyle(ChatFormatting.GOLD));
            if (!isEmptySlot) {
               list.add(
                  CreateLang.translateDirect("crafting_blueprint." + (((BlueprintMenu)this.menu).contentHolder.inferredIcon ? "inferred" : "manually_assigned"))
                     .withStyle(ChatFormatting.GRAY)
               );
            }
         } else if (slot == 10) {
            list.add(CreateLang.translateDirect("crafting_blueprint.secondary_display_slot").withStyle(ChatFormatting.GOLD));
            if (isEmptySlot) {
               list.add(CreateLang.translateDirect("crafting_blueprint.optional").withStyle(ChatFormatting.GRAY));
            }
         }

         return list;
      } else {
         return list;
      }
   }

   @Override
   protected void containerTick() {
      if (!((BlueprintMenu)this.menu).contentHolder.isEntityAlive()) {
         ((BlueprintMenu)this.menu).player.closeContainer();
      }

      super.containerTick();
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
