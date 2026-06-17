package net.createmod.catnip.config.ui.entries;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.createmod.catnip.config.ui.ConfigTextField;
import net.createmod.catnip.config.ui.HintableTextFieldWidget;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.element.TextStencilElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public abstract class NumberEntry<T extends Number> extends ValueEntry<T> {
   @Nullable
   protected TextStencilElement minText = null;
   @Nullable
   protected TextStencilElement maxText = null;
   protected int minOffset = 0;
   protected int maxOffset = 0;
   protected HintableTextFieldWidget textField = new ConfigTextField(Minecraft.getInstance().font, 0, 0, 200, 20);

   @Nullable
   public static NumberEntry<? extends Number> create(Object type, String label, ConfigValue<?> value, ValueSpec spec) {
      if (type instanceof Integer) {
         return new NumberEntry.IntegerEntry(label, (ConfigValue<Integer>)value, spec);
      } else if (type instanceof Float) {
         return new NumberEntry.FloatEntry(label, (ConfigValue<Float>)value, spec);
      } else {
         return type instanceof Double ? new NumberEntry.DoubleEntry(label, (ConfigValue<Double>)value, spec) : null;
      }
   }

   public NumberEntry(String label, ConfigValue<T> value, ValueSpec spec) {
      super(label, value, spec);
      if (this instanceof NumberEntry.IntegerEntry && this.annotations.containsKey("IntDisplay")) {
         String intDisplay = this.annotations.get("IntDisplay");
         int intValue = (Integer)this.getValue();

         String textValue = switch (intDisplay) {
            case "#" -> "#" + Integer.toHexString(intValue).toUpperCase(Locale.ROOT);
            case "0x" -> "0x" + Integer.toHexString(intValue).toUpperCase(Locale.ROOT);
            case "0b" -> "0b" + Integer.toBinaryString(intValue);
            default -> String.valueOf(intValue);
         };
         this.textField.setValue(textValue);
      } else {
         this.textField.setValue(String.valueOf(this.getValue()));
      }

      this.textField.setTextColor(UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
      Object range = spec.getRange();

      try {
         Field minField = range.getClass().getDeclaredField("min");
         Field maxField = range.getClass().getDeclaredField("max");
         minField.setAccessible(true);
         maxField.setAccessible(true);
         T min = (T)minField.get(range);
         T max = (T)maxField.get(range);
         Font font = Minecraft.getInstance().font;
         if (min.doubleValue() > this.getTypeMin().doubleValue()) {
            MutableComponent t = Component.literal(this.formatBound(min) + " < ");
            this.minText = new TextStencilElement(font, t).centered(true, false);
            this.minText
               .withElementRenderer(
                  (ms, width, height, alpha) -> UIRenderHelper.angledGradient(
                        ms, 0.0F, 0, height / 2, (float)height, (float)width, UIRenderHelper.COLOR_TEXT_DARKER
                     )
               );
            this.minOffset = font.width(t);
         }

         if (max.doubleValue() < this.getTypeMax().doubleValue()) {
            MutableComponent t = Component.literal(" < " + this.formatBound(max));
            this.maxText = new TextStencilElement(font, t).centered(true, false);
            this.maxText
               .withElementRenderer(
                  (ms, width, height, alpha) -> UIRenderHelper.angledGradient(
                        ms, 0.0F, 0, height / 2, (float)height, (float)width, UIRenderHelper.COLOR_TEXT_DARKER
                     )
               );
            this.maxOffset = font.width(t);
         }
      } catch (IllegalAccessException | ClassCastException | NullPointerException | NoSuchFieldException var11) {
      }

      this.textField.setResponder(s -> {
         try {
            T number = this.getParser().apply(s);
            if (!spec.test(number)) {
               throw new IllegalArgumentException();
            }

            this.textField.setTextColor(UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
            this.setValue(number);
         } catch (IllegalArgumentException var4x) {
            this.textField.setTextColor(AbstractSimiWidget.COLOR_FAIL.getFirst().getRGB());
         }
      });
      this.textField.moveCursorToStart(false);
      this.listeners.add(this.textField);
      this.onReset();
   }

   protected String formatBound(T bound) {
      String sci = String.format("%.2E", bound.doubleValue());
      String str = String.valueOf(bound);
      return sci.length() < str.length() ? sci : str;
   }

   protected abstract T getTypeMin();

   protected abstract T getTypeMax();

   protected abstract Function<String, T> getParser();

   @Override
   protected void setEditable(boolean b) {
      super.setEditable(b);
      this.textField.setEditable(b);
   }

   public void onValueChange(T newValue) {
      super.onValueChange(newValue);

      try {
         T current = this.getParser().apply(this.textField.getValue());
         if (!current.equals(newValue)) {
            this.textField.setValue(String.valueOf(newValue));
         }
      } catch (IllegalArgumentException var3) {
      }
   }

   @Override
   public void tick() {
      super.tick();
   }

   @Override
   public void render(GuiGraphics graphics, int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean p_230432_9_, float partialTicks) {
      super.render(graphics, index, y, x, width, height, mouseX, mouseY, p_230432_9_, partialTicks);
      this.textField.setX(x + width - 82 - 28);
      this.textField.setY(y + 8);
      this.textField.setWidth(Math.min(width - this.getLabelWidth(width) - 28 - this.minOffset - this.maxOffset, 40));
      this.textField.setHeight(20);
      this.textField.render(graphics, mouseX, mouseY, partialTicks);
      if (this.minText != null) {
         this.minText
            .<RenderElement>at((float)(this.textField.getX() - this.minOffset), (float)this.textField.getY(), 0.0F)
            .<RenderElement>withBounds(this.minOffset, this.textField.getHeight())
            .render(graphics);
      }

      if (this.maxText != null) {
         this.maxText
            .<RenderElement>at((float)(this.textField.getX() + this.textField.getWidth()), (float)this.textField.getY(), 0.0F)
            .<RenderElement>withBounds(this.maxOffset, this.textField.getHeight())
            .render(graphics);
      }
   }

   public static class DoubleEntry extends NumberEntry<Double> {
      public DoubleEntry(String label, ConfigValue<Double> value, ValueSpec spec) {
         super(label, value, spec);
      }

      protected Double getTypeMin() {
         return -Float.MAX_VALUE;
      }

      protected Double getTypeMax() {
         return Float.MAX_VALUE;
      }

      @Override
      protected Function<String, Double> getParser() {
         return Double::parseDouble;
      }
   }

   public static class FloatEntry extends NumberEntry<Float> {
      public FloatEntry(String label, ConfigValue<Float> value, ValueSpec spec) {
         super(label, value, spec);
      }

      protected Float getTypeMin() {
         return -Float.MAX_VALUE;
      }

      protected Float getTypeMax() {
         return Float.MAX_VALUE;
      }

      @Override
      protected Function<String, Float> getParser() {
         return Float::parseFloat;
      }
   }

   public static class IntegerEntry extends NumberEntry<Integer> {
      public IntegerEntry(String label, ConfigValue<Integer> value, ValueSpec spec) {
         super(label, value, spec);
      }

      protected Integer getTypeMin() {
         return Integer.MIN_VALUE;
      }

      protected Integer getTypeMax() {
         return Integer.MAX_VALUE;
      }

      @Override
      protected Function<String, Integer> getParser() {
         return string -> {
            if (string.startsWith("#")) {
               return Integer.parseUnsignedInt(string.substring(1), 16);
            } else if (string.startsWith("0x")) {
               return Integer.parseUnsignedInt(string.substring(2), 16);
            } else {
               return string.startsWith("0b") ? Integer.parseUnsignedInt(string.substring(2), 2) : Integer.parseInt(string);
            }
         };
      }
   }
}
