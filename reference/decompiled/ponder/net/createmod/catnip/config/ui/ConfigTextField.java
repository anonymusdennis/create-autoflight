package net.createmod.catnip.config.ui;

import net.minecraft.client.gui.Font;

public class ConfigTextField extends HintableTextFieldWidget {
   public ConfigTextField(Font font, int x, int y, int width, int height) {
      super(font, x, y, width, height);
   }

   @Override
   public boolean mouseClicked(double x, double y, int button) {
      if (!this.isMouseOver(x, y)) {
         this.setFocused(false);
      }

      return super.mouseClicked(x, y, button);
   }

   public void onClick(double pMouseX, double pMouseY) {
      super.onClick(pMouseX, pMouseY);
      this.setFocused(true);
   }

   public void setFocused(boolean focus) {
      super.setFocused(focus);
      if (!focus) {
         if (ConfigScreenList.currentText == this) {
            ConfigScreenList.currentText = null;
         }
      } else {
         if (ConfigScreenList.currentText != null && ConfigScreenList.currentText != this) {
            ConfigScreenList.currentText.setFocused(false);
         }

         ConfigScreenList.currentText = this;
      }
   }
}
