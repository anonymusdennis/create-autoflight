package com.simibubi.create.content.schematics.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ToolSelectionScreen extends Screen {
   public final String scrollToCycle = CreateLang.translateDirect("gui.toolmenu.cycle").getString();
   public final String holdToFocus = "gui.toolmenu.focusKey";
   protected List<ToolType> tools;
   protected Consumer<ToolType> callback;
   public boolean focused;
   private float yOffset;
   protected int selection;
   private boolean initialized;
   protected int w;
   protected int h;

   public ToolSelectionScreen(List<ToolType> tools, Consumer<ToolType> callback) {
      super(Component.literal("Tool Selection"));
      this.minecraft = Minecraft.getInstance();
      this.tools = tools;
      this.callback = callback;
      this.focused = false;
      this.yOffset = 0.0F;
      this.selection = 0;
      this.initialized = false;
      callback.accept(tools.get(this.selection));
      this.w = Math.max(tools.size() * 50 + 30, 220);
      this.h = 30;
   }

   public void setSelectedElement(ToolType tool) {
      if (this.tools.contains(tool)) {
         this.selection = this.tools.indexOf(tool);
      }
   }

   public void cycle(int direction) {
      this.selection += direction < 0 ? 1 : -1;
      this.selection = (this.selection + this.tools.size()) % this.tools.size();
   }

   private void draw(GuiGraphics graphics, float partialTicks) {
      PoseStack matrixStack = graphics.pose();
      Window mainWindow = this.minecraft.getWindow();
      if (!this.initialized) {
         this.init(this.minecraft, mainWindow.getGuiScaledWidth(), mainWindow.getGuiScaledHeight());
      }

      int x = (mainWindow.getGuiScaledWidth() - this.w) / 2 + 15;
      int y = mainWindow.getGuiScaledHeight() - this.h - 75;
      matrixStack.pushPose();
      matrixStack.translate(0.0F, -this.yOffset, this.focused ? 100.0F : 0.0F);
      AllGuiTextures gray = AllGuiTextures.HUD_BACKGROUND;
      RenderSystem.enableBlend();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.focused ? 0.875F : 0.5F);
      graphics.blit(gray.location, x - 15, y, (float)gray.getStartX(), (float)gray.getStartY(), this.w, this.h, gray.getWidth(), gray.getHeight());
      float toolTipAlpha = this.yOffset / 10.0F;
      List<Component> toolTip = this.tools.get(this.selection).getDescription();
      int stringAlphaComponent = (int)(toolTipAlpha * 255.0F) << 24;
      if (toolTipAlpha > 0.25F) {
         RenderSystem.setShaderColor(0.7F, 0.7F, 0.8F, toolTipAlpha);
         graphics.blit(gray.location, x - 15, y + 33, (float)gray.getStartX(), (float)gray.getStartY(), this.w, this.h + 22, gray.getWidth(), gray.getHeight());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         if (toolTip.size() > 0) {
            graphics.drawString(this.font, toolTip.get(0), x - 10, y + 38, 15658734 + stringAlphaComponent, false);
         }

         if (toolTip.size() > 1) {
            graphics.drawString(this.font, toolTip.get(1), x - 10, y + 50, 13426175 + stringAlphaComponent, false);
         }

         if (toolTip.size() > 2) {
            graphics.drawString(this.font, toolTip.get(2), x - 10, y + 60, 13426175 + stringAlphaComponent, false);
         }

         if (toolTip.size() > 3) {
            graphics.drawString(this.font, toolTip.get(3), x - 10, y + 72, 13421789 + stringAlphaComponent, false);
         }
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.tools.size() > 1) {
         String keyName = AllKeys.TOOL_MENU.getBoundKey();
         int width = this.minecraft.getWindow().getGuiScaledWidth();
         if (!this.focused) {
            graphics.drawCenteredString(this.minecraft.font, CreateLang.translateDirect("gui.toolmenu.focusKey", keyName), width / 2, y - 10, 13426175);
         } else {
            graphics.drawCenteredString(this.minecraft.font, this.scrollToCycle, width / 2, y - 10, 13426175);
         }
      } else {
         x += 65;
      }

      for (int i = 0; i < this.tools.size(); i++) {
         RenderSystem.enableBlend();
         matrixStack.pushPose();
         float alpha = this.focused ? 1.0F : 0.2F;
         if (i == this.selection) {
            matrixStack.translate(0.0F, -10.0F, 0.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.drawCenteredString(this.minecraft.font, this.tools.get(i).getDisplayName().getString(), x + i * 50 + 24, y + 28, 13426175);
            alpha = 1.0F;
         }

         RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, alpha);
         this.tools.get(i).getIcon().render(graphics, x + i * 50 + 16, y + 12);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
         this.tools.get(i).getIcon().render(graphics, x + i * 50 + 16, y + 11);
         matrixStack.popPose();
      }

      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.disableBlend();
      matrixStack.popPose();
   }

   public void update() {
      if (this.focused) {
         this.yOffset = this.yOffset + (10.0F - this.yOffset) * 0.1F;
      } else {
         this.yOffset *= 0.9F;
      }
   }

   public void renderPassive(GuiGraphics graphics, float partialTicks) {
      this.draw(graphics, partialTicks);
   }

   public void onClose() {
      this.callback.accept(this.tools.get(this.selection));
   }

   protected void init() {
      super.init();
      this.initialized = true;
   }
}
