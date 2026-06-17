package com.simibubi.create.content.schematics.client;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.GuiGameElement.GuiRenderBuilder;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public class SchematicEditScreen extends AbstractSimiScreen {
   private final List<Component> rotationOptions = CreateLang.translatedOptions("schematic.rotation", "none", "cw90", "cw180", "cw270");
   private final List<Component> mirrorOptions = CreateLang.translatedOptions("schematic.mirror", "none", "leftRight", "frontBack");
   private final Component rotationLabel = CreateLang.translateDirect("schematic.rotation");
   private final Component mirrorLabel = CreateLang.translateDirect("schematic.mirror");
   private AllGuiTextures background = AllGuiTextures.SCHEMATIC;
   private EditBox xInput;
   private EditBox yInput;
   private EditBox zInput;
   private IconButton confirmButton;
   private ScrollInput rotationArea;
   private ScrollInput mirrorArea;
   private SchematicHandler handler = CreateClient.SCHEMATIC_HANDLER;

   protected void init() {
      this.setWindowSize(this.background.getWidth(), this.background.getHeight());
      this.setWindowOffset(-6, 0);
      super.init();
      int x = this.guiLeft;
      int y = this.guiTop + 2;
      this.xInput = new EditBox(this.font, x + 50, y + 26, 34, 10, CommonComponents.EMPTY);
      this.yInput = new EditBox(this.font, x + 90, y + 26, 34, 10, CommonComponents.EMPTY);
      this.zInput = new EditBox(this.font, x + 130, y + 26, 34, 10, CommonComponents.EMPTY);
      BlockPos anchor = this.handler.getTransformation().getAnchor();
      if (this.handler.isDeployed()) {
         this.xInput.setValue(anchor.getX() + "");
         this.yInput.setValue(anchor.getY() + "");
         this.zInput.setValue(anchor.getZ() + "");
      } else {
         BlockPos alt = this.minecraft.player.blockPosition();
         this.xInput.setValue(alt.getX() + "");
         this.yInput.setValue(alt.getY() + "");
         this.zInput.setValue(alt.getZ() + "");
      }

      for (EditBox widget : new EditBox[]{this.xInput, this.yInput, this.zInput}) {
         widget.setMaxLength(6);
         widget.setBordered(false);
         widget.setTextColor(16777215);
         widget.setFocused(false);
         widget.mouseClicked(0.0, 0.0, 0);
         widget.setFilter(s -> {
            if (!s.isEmpty() && !s.equals("-")) {
               try {
                  Integer.parseInt(s);
                  return true;
               } catch (NumberFormatException var2x) {
                  return false;
               }
            } else {
               return true;
            }
         });
      }

      StructurePlaceSettings settings = this.handler.getTransformation().toSettings();
      Label labelR = new Label(x + 50, y + 48, CommonComponents.EMPTY).withShadow();
      this.rotationArea = new SelectionScrollInput(x + 45, y + 43, 118, 18)
         .forOptions(this.rotationOptions)
         .titled(this.rotationLabel.plainCopy())
         .setState(settings.getRotation().ordinal())
         .writingTo(labelR);
      Label labelM = new Label(x + 50, y + 70, CommonComponents.EMPTY).withShadow();
      this.mirrorArea = new SelectionScrollInput(x + 45, y + 65, 118, 18)
         .forOptions(this.mirrorOptions)
         .titled(this.mirrorLabel.plainCopy())
         .setState(settings.getMirror().ordinal())
         .writingTo(labelM);
      this.addRenderableWidgets(new EditBox[]{this.xInput, this.yInput, this.zInput});
      this.addRenderableWidgets(new AbstractSimiWidget[]{labelR, labelM, this.rotationArea, this.mirrorArea});
      this.confirmButton = new IconButton(x + this.background.getWidth() - 33, y + this.background.getHeight() - 26, AllIcons.I_CONFIRM);
      this.confirmButton.withCallback(() -> this.onClose());
      this.addRenderableWidget(this.confirmButton);
   }

   public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
      if (isPaste(code)) {
         String coords = this.minecraft.keyboardHandler.getClipboard();
         if (coords != null && !coords.isEmpty()) {
            coords.replaceAll(" ", "");
            String[] split = coords.split(",");
            if (split.length == 3) {
               boolean valid = true;

               for (String s : split) {
                  try {
                     Integer.parseInt(s);
                  } catch (NumberFormatException var12) {
                     valid = false;
                  }
               }

               if (valid) {
                  this.xInput.setValue(split[0]);
                  this.yInput.setValue(split[1]);
                  this.zInput.setValue(split[2]);
                  return true;
               }
            }
         }
      }

      return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      int x = this.guiLeft;
      int y = this.guiTop;
      this.background.render(graphics, x, y);
      String title = this.handler.getCurrentSchematicName();
      graphics.drawString(this.font, title, x + (this.background.getWidth() - 8 - this.font.width(title)) / 2, y + 4, 5263440, false);
      ((GuiRenderBuilder)GuiGameElement.of(AllItems.SCHEMATIC.asStack())
            .at((float)(x + this.background.getWidth() + 6), (float)(y + this.background.getHeight() - 40), -200.0F))
         .scale(3.0)
         .render(graphics);
   }

   public void removed() {
      boolean validCoords = true;
      BlockPos newLocation = null;

      try {
         newLocation = new BlockPos(
            Integer.parseInt(this.xInput.getValue()), Integer.parseInt(this.yInput.getValue()), Integer.parseInt(this.zInput.getValue())
         );
      } catch (NumberFormatException var5) {
         validCoords = false;
      }

      StructurePlaceSettings settings = new StructurePlaceSettings();
      settings.setRotation(Rotation.values()[this.rotationArea.getState()]);
      settings.setMirror(Mirror.values()[this.mirrorArea.getState()]);
      if (validCoords && newLocation != null) {
         ItemStack item = this.handler.getActiveSchematicItem();
         if (item != null) {
            item.set(AllDataComponents.SCHEMATIC_DEPLOYED, true);
            item.set(AllDataComponents.SCHEMATIC_ANCHOR, newLocation);
         }

         this.handler.getTransformation().init(newLocation, settings, this.handler.getBounds());
         this.handler.markDirty();
         this.handler.deploy();
      }
   }
}
