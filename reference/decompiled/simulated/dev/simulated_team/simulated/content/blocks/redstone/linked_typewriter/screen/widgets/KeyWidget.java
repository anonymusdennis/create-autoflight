package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterEntries;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.LinkedTypewriterScreen;
import dev.simulated_team.simulated.index.SimGUITextures;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class KeyWidget extends AbstractSimiWidget {
   private final LinkedTypewriterEntries.KeyboardEntry EMPTY;
   public int keyNum;
   public boolean bound;
   public boolean keyboardActive;
   private final ScreenElement icon;
   private final LinkedTypewriterScreen screen;

   public KeyWidget(int pX, int pY, int pWidth, int key, ScreenElement keyIcon, LinkedTypewriterScreen screen) {
      super(pX, pY, pWidth, 14, Component.empty());
      this.EMPTY = new LinkedTypewriterEntries.KeyboardEntry(Frequency.EMPTY, Frequency.EMPTY, this.keyNum, BlockPos.ZERO);
      this.bound = false;
      this.keyNum = key;
      this.icon = keyIcon;
      this.screen = screen;
   }

   public void render(GuiGraphics pGuiGraphics, int x, int y, int pMouseX, int pMouseY, float pPartialTick, boolean keyboardActive) {
      this.bound = this.screen.getNewEntries().getKeyMap().containsKey(this.keyNum);
      this.setX(x);
      this.setY(y);
      this.keyboardActive = keyboardActive;
      this.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
   }

   public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      SimGUITextures start = this.bound ? SimGUITextures.KEY_START : SimGUITextures.INACTIVE_KEY_START;
      SimGUITextures middle = this.bound ? SimGUITextures.KEY_MIDDLE : SimGUITextures.INACTIVE_KEY_MIDDLE;
      SimGUITextures end = this.bound ? SimGUITextures.KEY_END : SimGUITextures.INACTIVE_KEY_END;
      int midWidth = this.width - start.width - end.width;
      int endX = start.width + midWidth;
      boolean mouseHover = this.isMouseOver((double)mouseX, (double)mouseY);
      int y = this.getY() + (mouseHover && this.keyboardActive ? 2 : 0);
      start.render(graphics, this.getX(), y);

      for (int i = 0; i < midWidth / 2; i++) {
         middle.render(graphics, this.getX() + start.width + i * 2, y);
      }

      end.render(graphics, this.getX() + endX, y);
      if (this.icon != null) {
         if (this.bound) {
            RenderSystem.setShaderColor(0.447F, 0.278F, 0.192F, 1.0F);
         } else {
            RenderSystem.setShaderColor(0.318F, 0.125F, 0.094F, 1.0F);
         }

         this.icon.render(graphics, this.getX() + 3, y + 4);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      }

      if (this.isHovered) {
         this.renderHover(graphics, mouseX, mouseY, partialTicks);
      }
   }

   protected void renderHover(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      LinkedTypewriterEntries.KeyboardEntry keyboardEntry = this.screen.getNewEntries().getEntry(this.keyNum);
      if (keyboardEntry == null) {
         keyboardEntry = this.EMPTY;
      }

      if (this.keyboardActive) {
         SimGUITextures arrow = SimGUITextures.LINKED_TYPEWRITER_TOOLTIP_ARROW;
         SimGUITextures freq = SimGUITextures.LINKED_TYPEWRITER_FREQUENCY;
         Component keyName = this.keyName();
         int textLength = Minecraft.getInstance().font.width(keyName.getString());
         int textHeight = 9;
         int freqWidth = 20;
         int freqHeight = 20;
         int minWidth = arrow.width + 4 + 20;
         int bgWidth = Math.max(minWidth, textLength + 8);
         int bgHeight = textHeight + 12 + 20;
         int yOffset = 8;
         int textX = this.width / 2 - textLength / 2 + 1;
         int bgX = this.width / 2 - bgWidth / 2;
         int arrowX = this.width / 2 - arrow.width / 2;
         int freqX = this.width / 2 - freq.width / 2;
         this.renderBackground(pGuiGraphics, this.getX() + bgX, this.getY() - bgHeight - 8, bgWidth, bgHeight);
         arrow.render(pGuiGraphics, this.getX() + arrowX, this.getY() - 8 - 2);
         freq.render(pGuiGraphics, this.getX() + freqX, this.getY() - 8 - bgHeight + 4);
         Couple<Frequency> coupled = keyboardEntry.getAsCouple();
         pGuiGraphics.renderItem(((Frequency)coupled.getFirst()).getStack(), this.getX() + freqX + 1, this.getY() - 8 - bgHeight + 5);
         pGuiGraphics.renderItem(((Frequency)coupled.getSecond()).getStack(), this.getX() + freqX + 19, this.getY() - 8 - bgHeight + 5);
         pGuiGraphics.drawString(
            Minecraft.getInstance().font, keyName.getString(), this.getX() + textX - 1, this.getY() - 8 - textHeight - 2, DyeColor.BLACK.getTextColor(), false
         );
      }
   }

   private Component keyName() {
      return InputConstants.getKey(this.keyNum, GLFW.glfwGetKeyScancode(this.keyNum)).getDisplayName();
   }

   private void renderBackground(@NotNull GuiGraphics pGuiGraphics, int x, int y, int w, int h) {
      SimGUITextures bg = SimGUITextures.LINKED_TYPEWRITER_TOOLTIP_BACKGROUND;
      pGuiGraphics.blitSprite(bg.location, x, y, 0, w, h);
   }
}
