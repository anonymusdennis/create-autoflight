package com.simibubi.create.content.logistics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.schedule.DestinationSuggestions;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class AddressEditBox extends EditBox {
   private DestinationSuggestions destinationSuggestions;
   private Consumer<String> mainResponder;
   private String prevValue = "=)";

   public AddressEditBox(Screen screen, Font pFont, int pX, int pY, int pWidth, int pHeight, boolean anchorToBottom) {
      this(screen, pFont, pX, pY, pWidth, pHeight, anchorToBottom, null);
   }

   public AddressEditBox(Screen screen, Font pFont, int pX, int pY, int pWidth, int pHeight, boolean anchorToBottom, String localAddress) {
      super(pFont, pX, pY, pWidth, pHeight, Component.empty());
      this.destinationSuggestions = AddressEditBoxHelper.createSuggestions(screen, this, anchorToBottom, localAddress);
      this.destinationSuggestions.setAllowSuggestions(true);
      this.destinationSuggestions.updateCommandInfo();
      this.mainResponder = t -> {
         if (!t.equals(this.prevValue)) {
            this.destinationSuggestions.updateCommandInfo();
         }

         this.prevValue = t;
      };
      this.setResponder(this.mainResponder);
      this.setBordered(false);
      this.setFocused(false);
      this.mouseClicked(0.0, 0.0, 0);
      this.setMaxLength(25);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (this.destinationSuggestions.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else if (this.isFocused() && pKeyCode == 257) {
         this.setFocused(false);
         this.moveCursorToEnd(false);
         this.mouseClicked(0.0, 0.0, 0);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return this.destinationSuggestions.mouseScrolled(Mth.clamp(scrollY, -1.0, 1.0)) ? true : super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 1 && this.isMouseOver(pMouseX, pMouseY)) {
         this.setValue("");
         return true;
      } else {
         boolean wasFocused = this.isFocused();
         if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
            if (!wasFocused) {
               this.setHighlightPos(0);
               this.setCursorPosition(this.getValue().length());
            }

            return true;
         } else {
            return this.destinationSuggestions.mouseClicked((double)((int)pMouseX), (double)((int)pMouseY), pButton);
         }
      }
   }

   public void setValue(String text) {
      this.setHighlightPos(0);
      super.setValue(text);
   }

   public void setFocused(boolean focused) {
      super.setFocused(focused);
   }

   public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      PoseStack matrixStack = pGuiGraphics.pose();
      matrixStack.pushPose();
      matrixStack.translate(0.0F, 0.0F, 500.0F);
      this.destinationSuggestions.render(pGuiGraphics, pMouseX, pMouseY);
      matrixStack.popPose();
      if (this.destinationSuggestions.isEmpty()) {
         int itemX = this.getX() + this.width + 4;
         int itemY = this.getY() - 4;
         pGuiGraphics.renderItem(AllBlocks.CLIPBOARD.asStack(), itemX, itemY);
         if (pMouseX >= itemX && pMouseX < itemX + 16 && pMouseY >= itemY && pMouseY < itemY + 16) {
            List<Component> promiseTip = List.of();
            promiseTip = List.of(
               CreateLang.translate("gui.address_box.clipboard_tip").color(ScrollInput.HEADER_RGB).component(),
               CreateLang.translate("gui.address_box.clipboard_tip_1").style(ChatFormatting.GRAY).component(),
               CreateLang.translate("gui.address_box.clipboard_tip_2").style(ChatFormatting.GRAY).component(),
               CreateLang.translate("gui.address_box.clipboard_tip_3").style(ChatFormatting.GRAY).component(),
               CreateLang.translate("gui.address_box.clipboard_tip_4").style(ChatFormatting.DARK_GRAY).component()
            );
            pGuiGraphics.renderComponentTooltip(Minecraft.getInstance().font, promiseTip, pMouseX, pMouseY);
         }
      }
   }

   public void setResponder(Consumer<String> pResponder) {
      super.setResponder(pResponder == this.mainResponder ? this.mainResponder : this.mainResponder.andThen(pResponder));
   }

   public void tick() {
      if (!this.isFocused()) {
         this.destinationSuggestions.hide();
      }

      if (this.isFocused() && this.destinationSuggestions.suggestions == null) {
         this.destinationSuggestions.updateCommandInfo();
      }

      this.destinationSuggestions.tick();
   }
}
