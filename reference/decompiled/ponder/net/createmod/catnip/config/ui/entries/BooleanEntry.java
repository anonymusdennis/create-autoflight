package net.createmod.catnip.config.ui.entries;

import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public class BooleanEntry extends ValueEntry<Boolean> {
   RenderElement enabled = PonderGuiTextures.ICON_CONFIRM
      .asStencil()
      .<DelegatedStencilElement>withElementRenderer(
         (ms, width, height, alpha) -> UIRenderHelper.angledGradient(ms, 0.0F, 0, height / 2, (float)height, (float)width, AbstractSimiWidget.COLOR_SUCCESS)
      )
      .at(10.0F, 0.0F);
   RenderElement disabled = PonderGuiTextures.ICON_DISABLE
      .asStencil()
      .<DelegatedStencilElement>withElementRenderer(
         (ms, width, height, alpha) -> UIRenderHelper.angledGradient(ms, 0.0F, 0, height / 2, (float)height, (float)width, AbstractSimiWidget.COLOR_FAIL)
      )
      .at(10.0F, 0.0F);
   BoxWidget button = new BoxWidget().<ElementWidget>showingElement(this.enabled).withCallback(() -> this.setValue(Boolean.valueOf(!this.getValue())));

   public BooleanEntry(String label, ConfigValue<Boolean> value, ValueSpec spec) {
      super(label, value, spec);
      this.listeners.add(this.button);
      this.onReset();
   }

   @Override
   protected void setEditable(boolean b) {
      super.setEditable(b);
      this.button.active = b;
   }

   @Override
   public void tick() {
      super.tick();
      this.button.tick();
   }

   @Override
   public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
      super.render(graphics, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);
      this.button.setX(x + width - 80 - 28);
      this.button.setY(y + 10);
      this.button.setWidth(35);
      this.button.setHeight(height - 20);
      this.button.render(graphics, mouseX, mouseY, partialTicks);
   }

   public void onValueChange(Boolean newValue) {
      super.onValueChange(newValue);
      this.button.showingElement(newValue ? this.enabled : this.disabled);
      this.bumpCog(newValue ? 15.0F : -16.0F);
   }
}
