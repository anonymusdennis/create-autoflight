package dev.eriksonn.aeronautics.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.eriksonn.aeronautics.content.ponder.AeroPonderPlugin;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.lang.LangNumberFormat;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public class AeroLang {
   public static LangBuilder builder() {
      return Lang.builder("aeronautics");
   }

   public static LangBuilder text(String text) {
      return builder().text(text);
   }

   public static LangBuilder translate(String key, Object... args) {
      return builder().translate(key, args);
   }

   public static LangBuilder number(double number) {
      return builder().text(LangNumberFormat.format(number));
   }

   public static LangBuilder space() {
      return builder().space();
   }

   public static void emptyLine(List<Component> tooltip) {
      builder().text("").forGoggles(tooltip);
   }

   public static LangBuilder blockName(BlockState blockState) {
      return builder().add(blockState.getBlock().getName());
   }

   public static List<Component> translatedOptions(String prefix, String... keys) {
      List<Component> result = new ArrayList<>(keys.length);

      for (String key : keys) {
         result.add(translate((prefix != null ? prefix + "." : "") + key).component());
      }

      return result;
   }

   public static LangBuilder kilopixelGram(double value) {
      return translate("unit.kpg", String.format("%,.2f", value));
   }

   public static LangBuilder kilopixelGram(double value, String format) {
      return getPrefixedUnit("pg", value, format, 1);
   }

   public static LangBuilder pixelNewton(double value) {
      return pixelNewton(value, "%,.2f");
   }

   public static LangBuilder pixelNewton(double value, String format) {
      return getPrefixedUnit("pn", value, format, 0);
   }

   private static LangBuilder getPrefixedUnit(String unit, double value, String format, int offset) {
      String[] prefixes = new String[]{"k", "m", "g"};

      int index;
      for (index = offset - 1; value >= 1000.0 && index < prefixes.length - 1; index++) {
         value /= 1000.0;
      }

      if (index >= 0) {
         unit = prefixes[index] + unit;
      }

      return translate("unit." + unit, format.formatted(value));
   }

   public static void registrateLang(RegistrateLangProvider provider) {
      BiConsumer<String, String> consumer = provider::add;
      Map<String, String> lang = getLangMap("en_us");
      lang.forEach(consumer);
      AeroAdvancements.provideLang(consumer);
      AeroSoundEvents.REGISTRY.provideLang(consumer);
      PonderIndex.addPlugin(new AeroPonderPlugin());
      PonderIndex.getLangAccess().provideLang("aeronautics", consumer);
   }

   private static Map<String, String> getLangMap(String lang) {
      String filepath = "datagen/lang/%s.json".formatted(lang);
      JsonObject langObject = FilesHelper.loadJsonResource(filepath).getAsJsonObject();
      Map<String, String> langMap = new HashMap<>();
      flattenJson(langMap, langObject, null);
      return langMap;
   }

   private static void flattenJson(Map<String, String> outputMap, JsonElement element, String currentPath) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
         String string = element.getAsJsonPrimitive().getAsString();
         outputMap.put(currentPath, string);
      } else {
         if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();

            for (String key : object.keySet()) {
               JsonElement value = object.get(key);
               String path = currentPath != null ? currentPath + "." + key : key;
               flattenJson(outputMap, value, path);
            }
         }
      }
   }
}
