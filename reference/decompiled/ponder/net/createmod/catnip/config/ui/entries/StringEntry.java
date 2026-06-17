package net.createmod.catnip.config.ui.entries;

import net.createmod.catnip.config.ui.ConfigTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public class StringEntry extends ValueEntry<String> {
   protected EditBox textField = new ConfigTextField(Minecraft.getInstance().font, 0, 0, 200, 20);

   public StringEntry(String label, ConfigValue<String> value, ValueSpec spec) {
      super(label, value, spec);
      this.textField.setValue((String)value.get());
      this.textField.setResponder(this::setValue);
      this.textField.moveCursorToStart(false);
      this.listeners.add(this.textField);
      this.onReset();
   }

   @Override
   public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
      super.render(graphics, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);
      this.textField.setX(x + width - 82 - 28);
      this.textField.setY(y + 8);
      this.textField.setWidth(Math.min(width - this.getLabelWidth(width) - 28, 60));
      this.textField.render(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   protected void setEditable(boolean b) {
      super.setEditable(b);
      this.textField.setEditable(b);
   }
}
