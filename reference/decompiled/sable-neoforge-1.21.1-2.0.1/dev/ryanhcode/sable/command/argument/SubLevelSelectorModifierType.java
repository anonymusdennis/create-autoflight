package dev.ryanhcode.sable.command.argument;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class SubLevelSelectorModifierType {
   private static final Map<String, SubLevelSelectorModifierType> MODIFIERS_BY_NAME = new Object2ObjectOpenHashMap();
   private static final SimpleCommandExceptionType UNKNOWN_PROPERTY_NAME = new SimpleCommandExceptionType(
      Component.translatable("argument.sable.sub_level.unknown_property")
   );
   private final String name;
   private final SubLevelSelectorModifierType.Parser parser;
   private final SubLevelSelectorModifierType.FilterPriority filterPriority;

   public SubLevelSelectorModifierType(String name, SubLevelSelectorModifierType.Parser parser, SubLevelSelectorModifierType.FilterPriority priority) {
      this.name = name;
      this.parser = parser;
      this.filterPriority = priority;
   }

   public static void registerType(String name, SubLevelSelectorModifierType.Parser parser, SubLevelSelectorModifierType.FilterPriority filterPriority) {
      if (MODIFIERS_BY_NAME.containsKey(name)) {
         throw new IllegalArgumentException("Modifier type " + name + " already registered");
      } else {
         MODIFIERS_BY_NAME.put(name, new SubLevelSelectorModifierType(name, parser, filterPriority));
      }
   }

   public static SubLevelSelectorModifierType getModifier(String propertyName, StringReader readerForErrorContext) throws CommandSyntaxException {
      if (!MODIFIERS_BY_NAME.containsKey(propertyName)) {
         throw UNKNOWN_PROPERTY_NAME.createWithContext(readerForErrorContext);
      } else {
         return MODIFIERS_BY_NAME.get(propertyName);
      }
   }

   public static void clearRegistry() {
      MODIFIERS_BY_NAME.clear();
   }

   public static List<Pair<String, Message>> getAllNamesWithTooltip() {
      ArrayList<Pair<String, Message>> modifiers = new ArrayList<>();

      for (SubLevelSelectorModifierType modifier : MODIFIERS_BY_NAME.values()) {
         modifiers.add(Pair.of(modifier.name, Component.translatable("argument.sable.sub_level.modifier." + modifier.name)));
      }

      return modifiers;
   }

   public SubLevelSelectorModifierType.Parser getParser() {
      return this.parser;
   }

   public SubLevelSelectorModifierType.FilterPriority getFilterPriority() {
      return this.filterPriority;
   }

   public static enum FilterPriority {
      POSITION,
      FILTER,
      SORTING,
      SORTING_SELECTION;
   }

   public interface Modifier {
      int getMaxResults();

      @Nullable
      List<ServerSubLevel> apply(List<ServerSubLevel> var1, Vector3d var2);
   }

   public interface Parser {
      SubLevelSelectorModifierType.Modifier parse(StringReader var1) throws CommandSyntaxException;
   }
}
