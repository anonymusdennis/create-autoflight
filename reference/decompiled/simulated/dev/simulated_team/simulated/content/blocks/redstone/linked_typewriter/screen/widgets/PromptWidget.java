package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets;

import com.mojang.blaze3d.platform.InputConstants;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.EntryModifierScreen;
import dev.simulated_team.simulated.data.SimLang;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class PromptWidget extends AbstractSimiWidget {
   private final EntryModifierScreen entryModifierScreen;
   protected boolean bindingActive = false;

   public PromptWidget(EntryModifierScreen entryModifierScreen, int x, int y, int width, int height) {
      super(x, y, width, height);
      this.entryModifierScreen = entryModifierScreen;
   }

   protected void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.doRender(graphics, mouseX, mouseY, partialTicks);
      if (this.entryModifierScreen.modifying && this.entryModifierScreen.psuedoEntry != null) {
         Component displayName = InputConstants.getKey(this.entryModifierScreen.psuedoEntry.glfwKeyCode, -1).getDisplayName();
         if (this.bindingActive) {
            displayName = SimLang.translate("linked_typewriter.bind_screen_prompt").component();
         } else if (this.entryModifierScreen.psuedoEntry.glfwKeyCode == -1) {
            displayName = SimLang.translate("linked_typewriter.bind_new_key").component();
         }

         graphics.pose().translate(3.0F, 4.0F, 0.0F);
         graphics.drawString(Minecraft.getInstance().font, displayName, this.getX(), this.getY(), 16777215, true);
      }
   }

   public void onClick(double mouseX, double mouseY) {
      super.onClick(mouseX, mouseY);
      this.bindingActive ^= true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.bindingActive && this.entryModifierScreen.psuedoEntry != null) {
         this.entryModifierScreen.psuedoEntry.keyCode(keyCode);
         this.bindingActive = false;
         return true;
      } else {
         this.bindingActive = false;
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }
}
