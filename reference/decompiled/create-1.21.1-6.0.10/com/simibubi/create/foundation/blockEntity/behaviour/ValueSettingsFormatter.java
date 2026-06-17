package com.simibubi.create.foundation.blockEntity.behaviour;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ValueSettingsFormatter {
   private final Function<ValueSettingsBehaviour.ValueSettings, MutableComponent> formatter;

   public ValueSettingsFormatter(Function<ValueSettingsBehaviour.ValueSettings, MutableComponent> formatter) {
      this.formatter = formatter;
   }

   public MutableComponent format(ValueSettingsBehaviour.ValueSettings valueSettings) {
      return this.formatter.apply(valueSettings);
   }

   public static class ScrollOptionSettingsFormatter extends ValueSettingsFormatter {
      private final INamedIconOptions[] options;

      public ScrollOptionSettingsFormatter(INamedIconOptions[] options) {
         super(v -> Component.translatable(options[v.value()].getTranslationKey()));
         this.options = options;
      }

      public AllIcons getIcon(ValueSettingsBehaviour.ValueSettings valueSettings) {
         return this.options[valueSettings.value()].getIcon();
      }
   }
}
