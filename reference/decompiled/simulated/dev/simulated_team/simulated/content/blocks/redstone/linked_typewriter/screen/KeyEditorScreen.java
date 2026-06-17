package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets.ConfirmationWidgetBase;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimIcons;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public class KeyEditorScreen {
   private static final SimGUITextures KEY_MENU = SimGUITextures.LINKED_TYPEWRITER_KEYS_MENU;
   private static final SimGUITextures KEY_ENTRY = SimGUITextures.LINKED_TYPEWRITER_KEY_ENTRY;
   private static final int MIN_SCROLL_Y = 50;
   private static final int ENTRY_HEIGHT_PADDING_PIXELS = 3;
   private final LinkedTypewriterScreen parentScreen;
   public boolean active;
   private int scroll = 0;
   private final LerpedFloat lerpedScroll = LerpedFloat.linear();
   ObjectArrayList<KeyEditorScreen.KeyEntryWidget> keyboardEntryWrappers = new ObjectArrayList();
   private final IconButton addWidget;
   private final IconButton confirmWidget;
   private final IconButton removeAllWidget;

   public KeyEditorScreen(LinkedTypewriterScreen parentScreen) {
      this.parentScreen = parentScreen;
      this.addWidget = (IconButton)new IconButton(0, 0, AllIcons.I_ADD).withCallback(() -> this.modifyEntry(null));
      this.confirmWidget = (IconButton)new IconButton(0, 0, AllIcons.I_CONFIRM).withCallback(() -> this.parentScreen.switchScreen(false));
      this.removeAllWidget = (IconButton)new ConfirmationWidgetBase(0, 0, AllIcons.I_TRASH)
         .<ConfirmationWidgetBase>withMessage(Component.translatable("simulated.linked_typewriter.confirm_delete_all"))
         .withCallback(() -> parentScreen.sendNewKeys(true));
      this.resetPositions();
   }

   public void startEditing() {
      this.resetPositions();
      this.addAllWidgets();
      this.rebuildWrappers();
      this.active = true;
   }

   public void endEditing() {
      this.removeAllWidgets();
      this.keyboardEntryWrappers.clear();
      this.active = false;
   }

   public void resetPositions() {
      int widgetHeight = this.topPos() + KEY_MENU.height - 24;
      this.addWidget.setX(this.leftPos() + KEY_MENU.width - 54);
      this.addWidget.setY(widgetHeight);
      this.confirmWidget.setX(this.leftPos() + KEY_MENU.width - 25);
      this.confirmWidget.setY(widgetHeight);
      this.removeAllWidget.setX(this.leftPos() + 8);
      this.removeAllWidget.setY(widgetHeight);
   }

   public void activateAllWidgets() {
      this.addWidget.active = true;
      this.confirmWidget.active = true;
      this.removeAllWidget.active = true;
      ObjectListIterator var1 = this.keyboardEntryWrappers.iterator();

      while (var1.hasNext()) {
         KeyEditorScreen.KeyEntryWidget wrapper = (KeyEditorScreen.KeyEntryWidget)var1.next();
         wrapper.editWidget.active = true;
         wrapper.deleteWidget.active = true;
      }
   }

   public void deactivateAllWidgets() {
      this.addWidget.active = false;
      this.confirmWidget.active = false;
      this.removeAllWidget.active = false;
      ObjectListIterator var1 = this.keyboardEntryWrappers.iterator();

      while (var1.hasNext()) {
         KeyEditorScreen.KeyEntryWidget wrapper = (KeyEditorScreen.KeyEntryWidget)var1.next();
         wrapper.editWidget.active = false;
         wrapper.deleteWidget.active = false;
      }
   }

   public void addAllWidgets() {
      this.parentScreen.addWidget(this.addWidget);
      this.parentScreen.addWidget(this.confirmWidget);
      this.parentScreen.addWidget(this.removeAllWidget);
      ObjectListIterator var1 = this.keyboardEntryWrappers.iterator();

      while (var1.hasNext()) {
         KeyEditorScreen.KeyEntryWidget wrapper = (KeyEditorScreen.KeyEntryWidget)var1.next();
         this.parentScreen.addWidget(wrapper.editWidget);
         this.parentScreen.addWidget(wrapper.deleteWidget);
      }
   }

   public void removeAllWidgets() {
      this.parentScreen.removeWidget(this.addWidget);
      this.parentScreen.removeWidget(this.confirmWidget);
      this.parentScreen.removeWidget(this.removeAllWidget);
      ObjectListIterator var1 = this.keyboardEntryWrappers.iterator();

      while (var1.hasNext()) {
         KeyEditorScreen.KeyEntryWidget wrapper = (KeyEditorScreen.KeyEntryWidget)var1.next();
         this.parentScreen.removeWidget(wrapper.editWidget);
         this.parentScreen.removeWidget(wrapper.deleteWidget);
      }
   }

   public void tick() {
      this.lerpedScroll.chase((double)this.scroll, 0.8, Chaser.EXP);
      this.lerpedScroll.tickChaser();
      this.clampScroll();
   }

   protected void shiftEntries(boolean shiftLeft) {
      int shiftBy = shiftLeft ? -1 : 1;
      this.scroll += shiftBy * 19;
      this.clampScroll();
   }

   private void clampScroll() {
      int maxScroll = Math.max(0, (this.parentScreen.getNewEntries().getSize() - 4) * (SimGUITextures.LINKED_TYPEWRITER_KEY_ENTRY.height + 3));
      this.scroll = Math.clamp((long)this.scroll, 0, maxScroll);
   }

   private void addEntry(LinkedTypewriterEntries.KeyboardEntry entryFromModifier) {
      this.parentScreen.getNewEntries().setKey(entryFromModifier.glfwKeyCode, entryFromModifier);
      this.rebuildWrappers();
   }

   private void removeWidget(KeyEditorScreen.KeyEntryWidget wrapper) {
      LinkedTypewriterEntries.KeyboardEntry entry = wrapper.entry;
      if (entry != null) {
         this.parentScreen.getNewEntries().setKey(entry.glfwKeyCode, null);
      }

      this.rebuildWrappers();
   }

   private void modifyEntry(@Nullable KeyEditorScreen.KeyEntryWidget widget) {
      this.parentScreen.modifier.startModifying(widget == null ? null : widget.entry, newEntry -> {
         if (newEntry != null) {
            KeyEditorScreen.KeyEntryWidget alreadyPresent = null;
            ObjectListIterator var3 = this.keyboardEntryWrappers.iterator();

            while (var3.hasNext()) {
               KeyEditorScreen.KeyEntryWidget wrapperEntry = (KeyEditorScreen.KeyEntryWidget)var3.next();
               if (wrapperEntry.entry.glfwKeyCode == newEntry.glfwKeyCode) {
                  alreadyPresent = wrapperEntry;
                  break;
               }
            }

            if (alreadyPresent != null) {
               this.parentScreen.getNewEntries().setKey(newEntry.glfwKeyCode, newEntry);
               alreadyPresent.entry = newEntry;
            } else {
               this.addEntry(newEntry);
            }
         }

         this.activateAllWidgets();
      });
      if (widget != null) {
         this.removeWidget(widget);
      }

      this.deactivateAllWidgets();
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pt, PoseStack ps) {
      guiGraphics.enableScissor(0, this.topPos() + 20, this.parentScreen.width, this.topPos() + KEY_MENU.height - 35);
      ObjectListIterator fadeOffColor = this.keyboardEntryWrappers.iterator();

      while (fadeOffColor.hasNext()) {
         KeyEditorScreen.KeyEntryWidget wrapper = (KeyEditorScreen.KeyEntryWidget)fadeOffColor.next();
         wrapper.render(guiGraphics, mouseX, mouseY, pt, ps);
      }

      guiGraphics.disableScissor();
      this.addWidget.render(guiGraphics, mouseX, mouseY, pt);
      this.confirmWidget.render(guiGraphics, mouseX, mouseY, pt);
      this.removeAllWidget.render(guiGraphics, mouseX, mouseY, pt);
      int fadeOffColorx = 0;
      int fadeFromColor = 1996488704;
      guiGraphics.fillGradient(this.leftPos() + 7, this.topPos() + 20, this.leftPos() + 231, this.topPos() + 30, 1996488704, 0);
      guiGraphics.fillGradient(this.leftPos() + 7, this.topPos() + 150, this.leftPos() + 231, this.topPos() + 160, 0, 1996488704);
   }

   public void rebuildWrappers() {
      ObjectListIterator entries = this.keyboardEntryWrappers.iterator();

      while (entries.hasNext()) {
         KeyEditorScreen.KeyEntryWidget wrapper = (KeyEditorScreen.KeyEntryWidget)entries.next();
         this.parentScreen.removeWidget(wrapper.deleteWidget);
         this.parentScreen.removeWidget(wrapper.editWidget);
      }

      this.keyboardEntryWrappers.clear();
      LinkedTypewriterEntries entriesx = this.parentScreen.getNewEntries();

      for (LinkedTypewriterEntries.KeyboardEntry entry : entriesx.getEntries()) {
         KeyEditorScreen.KeyEntryWidget wrapper = new KeyEditorScreen.KeyEntryWidget(entry);
         this.keyboardEntryWrappers.add(wrapper);
         this.parentScreen.addWidget(wrapper.editWidget);
         this.parentScreen.addWidget(wrapper.deleteWidget);
      }
   }

   public void renderBG(GuiGraphics guiGraphics, float v, int i, int i1) {
      KEY_MENU.render(guiGraphics, this.leftPos(), this.topPos());
      guiGraphics.enableScissor(0, this.topPos() + 20, this.parentScreen.width, this.topPos() + KEY_MENU.height - 35);
      ObjectListIterator var5 = this.keyboardEntryWrappers.iterator();

      while (var5.hasNext()) {
         KeyEditorScreen.KeyEntryWidget wrapper = (KeyEditorScreen.KeyEntryWidget)var5.next();
         wrapper.renderBackground(guiGraphics, v, i, i1);
      }

      guiGraphics.disableScissor();
   }

   private int leftPos() {
      return this.parentScreen.getLeftPos();
   }

   private int topPos() {
      return this.parentScreen.getTopPos() - 40;
   }

   private class KeyEntryWidget {
      private final KeyEditorScreen.NoXYButton editWidget = (KeyEditorScreen.NoXYButton)new KeyEditorScreen.NoXYButton(SimIcons.ADD_OR_EDIT)
         .withCallback(() -> KeyEditorScreen.this.modifyEntry(this));
      private final KeyEditorScreen.NoXYButton deleteWidget = (KeyEditorScreen.NoXYButton)new KeyEditorScreen.NoXYButton(AllIcons.I_TRASH)
         .withCallback(() -> KeyEditorScreen.this.removeWidget(this));
      private LinkedTypewriterEntries.KeyboardEntry entry;

      public KeyEntryWidget(final LinkedTypewriterEntries.KeyboardEntry entry) {
         this.entry = entry;
      }

      private float getCurrentHeight(float partialTick) {
         int index = KeyEditorScreen.this.keyboardEntryWrappers.indexOf(this);
         return (float)(KeyEditorScreen.this.parentScreen.getTopPos() - 65 + 50)
            - KeyEditorScreen.this.lerpedScroll.getValue(partialTick)
            + (float)(index * (SimGUITextures.LINKED_TYPEWRITER_KEY_ENTRY.height + 3));
      }

      public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float pt, PoseStack ps) {
         ps.pushPose();
         int x = KeyEditorScreen.this.leftPos() + 12;
         float y = this.getCurrentHeight(pt);
         ps.translate((float)x, y, 0.0F);
         int editIconOffset = 167;
         float iconY = (float)KeyEditorScreen.KEY_ENTRY.height / 2.0F;
         this.updateWidgetPositions(x, 167, y, iconY);
         this.renderItems(guiGraphics, ps);
         ps.popPose();
      }

      public void renderBackground(GuiGraphics guiGraphics, float pt, int mouseX, int mouseY) {
         PoseStack ps = guiGraphics.pose();
         int editIconOffset = 167;
         float iconY = (float)KeyEditorScreen.KEY_ENTRY.height / 2.0F;
         ps.pushPose();
         int x = KeyEditorScreen.this.leftPos() + 12;
         float y = this.getCurrentHeight(pt);
         ps.translate((float)x, y, 0.0F);
         KeyEditorScreen.KEY_ENTRY.render(guiGraphics, 0, 0);
         this.renderText(guiGraphics, ps);
         this.renderWidgets(guiGraphics, mouseX, mouseY, pt, ps, 167, iconY);
         ps.popPose();
      }

      private void updateWidgetPositions(int x, int editIconOffset, float y, float iconY) {
         this.editWidget.setX(x + editIconOffset);
         this.editWidget.setY((int)(y + iconY - 9.0F));
         this.deleteWidget.setX(x + editIconOffset + 23);
         this.deleteWidget.setY((int)(y + iconY - 9.0F));
      }

      private void renderItems(GuiGraphics guiGraphics, PoseStack ps) {
         if (!KeyEditorScreen.this.parentScreen.modifier.modifying) {
            ps.pushPose();
            ps.translate(0.0F, (float)KeyEditorScreen.KEY_ENTRY.height / 2.0F - 8.0F, 0.0F);
            ps.translate(82.0F, 0.0F, 0.0F);
            GuiGameElement.of(this.entry.getFirstAsItemStack()).render(guiGraphics);
            ps.translate(18.0F, 0.0F, 0.0F);
            GuiGameElement.of(this.entry.getSecondAsItemStack()).render(guiGraphics);
            ps.popPose();
         }
      }

      private void renderText(GuiGraphics guiGraphics, PoseStack ps) {
         ps.pushPose();
         ps.translate(9.0F, 11.0F, 0.0F);
         guiGraphics.drawString(Minecraft.getInstance().font, InputConstants.getKey(this.entry.glfwKeyCode, -1).getDisplayName(), 0, 0, 16777215, true);
         ps.popPose();
      }

      private void renderWidgets(GuiGraphics guiGraphics, int mouseX, int mouseY, float pt, PoseStack ps, int editIconOffset, float iconY) {
         ps.pushPose();
         ps.translate((float)editIconOffset, iconY - 9.0F, 0.0F);
         this.editWidget.render(guiGraphics, mouseX, mouseY, pt);
         ps.translate(23.0F, 0.0F, 0.0F);
         this.deleteWidget.render(guiGraphics, mouseX, mouseY, pt);
         ps.popPose();
      }
   }

   public static class NoXYButton extends IconButton {
      public NoXYButton(ScreenElement icon) {
         super(0, 0, icon);
      }

      public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
         if (this.visible) {
            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
            AllGuiTextures button = !this.active
               ? AllGuiTextures.BUTTON_DISABLED
               : (
                  this.isHovered && AllKeys.isMouseButtonDown(0)
                     ? AllGuiTextures.BUTTON_DOWN
                     : (this.isHovered ? AllGuiTextures.BUTTON_HOVER : (this.green ? AllGuiTextures.BUTTON_GREEN : AllGuiTextures.BUTTON))
               );
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.blit(button.location, 0, 0, button.getStartX(), button.getStartY(), button.getWidth(), button.getHeight());
            this.icon.render(graphics, 1, 1);
         }
      }
   }
}
