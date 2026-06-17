package com.simibubi.create.content.schematics.client;

import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class SchematicPromptScreen extends AbstractSimiScreen {
   private AllGuiTextures background;
   private final Component convertLabel = CreateLang.translateDirect("schematicAndQuill.convert");
   private final Component abortLabel = CreateLang.translateDirect("action.discard");
   private final Component confirmLabel = CreateLang.translateDirect("action.saveToFile");
   private EditBox nameField;
   private IconButton confirm;
   private IconButton abort;
   private IconButton convert;

   public SchematicPromptScreen() {
      super(CreateLang.translateDirect("schematicAndQuill.title"));
      this.background = AllGuiTextures.SCHEMATIC_PROMPT;
   }

   public void init() {
      this.setWindowSize(this.background.getWidth(), this.background.getHeight());
      super.init();
      int x = this.guiLeft;
      int y = this.guiTop + 2;
      this.nameField = new EditBox(this.font, x + 49, y + 26, 131, 10, CommonComponents.EMPTY);
      this.nameField.setTextColor(-1);
      this.nameField.setTextColorUneditable(-1);
      this.nameField.setBordered(false);
      this.nameField.setMaxLength(35);
      this.nameField.setFocused(true);
      this.setFocused(this.nameField);
      this.addRenderableWidget(this.nameField);
      this.abort = new IconButton(x + 7, y + 53, AllIcons.I_TRASH);
      this.abort.withCallback(() -> {
         CreateClient.SCHEMATIC_AND_QUILL_HANDLER.discard();
         this.onClose();
      });
      this.abort.setToolTip(this.abortLabel);
      this.addRenderableWidget(this.abort);
      this.confirm = new IconButton(x + 158, y + 53, AllIcons.I_CONFIRM);
      this.confirm.withCallback(() -> this.confirm(false));
      this.confirm.setToolTip(this.confirmLabel);
      this.addRenderableWidget(this.confirm);
      this.convert = new IconButton(x + 180, y + 53, AllIcons.I_SCHEMATIC);
      this.convert.withCallback(() -> this.confirm(true));
      this.convert.setToolTip(this.convertLabel);
      this.addRenderableWidget(this.convert);
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      int x = this.guiLeft;
      int y = this.guiTop;
      this.background.render(graphics, x, y);
      graphics.drawString(this.font, this.title, x + (this.background.getWidth() - 8 - this.font.width(this.title)) / 2, y + 4, 5263440, false);
      GuiGameElement.of(AllItems.SCHEMATIC.asStack()).at((float)(x + 22), (float)(y + 24), 0.0F).render(graphics);
      GuiGameElement.of(AllItems.SCHEMATIC_AND_QUILL.asStack())
         .scale(3.0)
         .at((float)(x + this.background.getWidth() + 6), (float)(y + this.background.getHeight() - 38), -200.0F)
         .render(graphics);
   }

   public boolean keyPressed(int keyCode, int p_keyPressed_2_, int p_keyPressed_3_) {
      if (keyCode == 257) {
         this.confirm(false);
         return true;
      } else if (keyCode == 256 && this.shouldCloseOnEsc()) {
         this.onClose();
         return true;
      } else {
         return this.nameField.keyPressed(keyCode, p_keyPressed_2_, p_keyPressed_3_);
      }
   }

   private void confirm(boolean convertImmediately) {
      CreateClient.SCHEMATIC_AND_QUILL_HANDLER.saveSchematic(this.nameField.getValue(), convertImmediately);
      this.onClose();
   }
}
