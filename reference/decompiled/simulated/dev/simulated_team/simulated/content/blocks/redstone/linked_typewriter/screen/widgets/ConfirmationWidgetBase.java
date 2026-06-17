package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.screen.widgets;

import com.simibubi.create.foundation.gui.widget.IconButton;
import java.util.List;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;

public class ConfirmationWidgetBase extends IconButton {
   public boolean confirmation;
   public MutableComponent message;

   public ConfirmationWidgetBase(int x, int y, ScreenElement icon) {
      super(x, y, icon);
   }

   public <T extends ConfirmationWidgetBase> T withMessage(MutableComponent component) {
      this.message = component;
      return (T)this;
   }

   public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.doRender(graphics, mouseX, mouseY, partialTicks);
      if (this.isHovered && this.visible && this.active && this.confirmation) {
         this.renderHoveredText(graphics, mouseX, mouseY);
      }
   }

   public void renderHoveredText(GuiGraphics graphics, int mouseX, int mouseY) {
      graphics.renderComponentTooltip(Minecraft.getInstance().font, List.of(this.message.withColor(16711680)), mouseX, mouseY);
   }

   protected boolean clicked(double mouseX, double mouseY) {
      if (!this.isMouseOver(mouseX, mouseY)) {
         this.confirmation = false;
      }

      return super.clicked(mouseX, mouseY);
   }

   public void onClick(double mouseX, double mouseY) {
      if (this.confirmation) {
         this.runCallback(mouseX, mouseY);
         this.confirmation = false;
      } else {
         this.confirmation = true;
      }
   }
}
