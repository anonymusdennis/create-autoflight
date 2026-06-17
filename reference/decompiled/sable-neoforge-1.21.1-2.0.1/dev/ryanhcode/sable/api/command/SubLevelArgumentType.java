package dev.ryanhcode.sable.api.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.ryanhcode.sable.command.argument.SubLevelSelector;
import dev.ryanhcode.sable.command.argument.SubLevelSelectorModifierType;
import dev.ryanhcode.sable.command.argument.SubLevelSelectorType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SubLevelArgumentType implements ArgumentType<SubLevelSelector> {
   public static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> NO_SUGGESTIONS = SuggestionsBuilder::buildFuture;
   private static final SimpleCommandExceptionType ERROR_SINGLE_SUB_LEVEL_REQUIRED = new SimpleCommandExceptionType(
      Component.translatable("argument.sable.single_sub_level_required")
   );
   private static final SimpleCommandExceptionType ERROR_INVALID_SUBLEVEL = new SimpleCommandExceptionType(
      Component.translatable("argument.sable.sub_level.invalid")
   );
   private static final SimpleCommandExceptionType UNEXPECTED_END_OF_INPUT = new SimpleCommandExceptionType(
      Component.translatable("argument.sable.unexpected_end_of_input")
   );
   private static final String STATIC_WORLD = "static_world";
   private static final Collection<String> EXAMPLES = Arrays.stream(SubLevelSelectorType.values()).map(type -> "@" + type.getChar()).toList();
   private static Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = NO_SUGGESTIONS;
   private final boolean allowStaticLevel;
   private final boolean allowMultiple;

   public SubLevelArgumentType(boolean allowStaticLevel, boolean allowMultiple) {
      this.allowStaticLevel = allowStaticLevel;
      this.allowMultiple = allowMultiple;
   }

   public static Collection<ServerSubLevel> getSubLevels(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
      return ((SubLevelSelector)ctx.getArgument(name, SubLevelSelector.class)).getSubLevels((CommandSourceStack)ctx.getSource());
   }

   public static ServerSubLevel getSingleSubLevel(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
      Collection<ServerSubLevel> subLevels = ((SubLevelSelector)ctx.getArgument(name, SubLevelSelector.class))
         .getSubLevels((CommandSourceStack)ctx.getSource());
      if (subLevels.size() > 1) {
         throw ERROR_SINGLE_SUB_LEVEL_REQUIRED.create();
      } else if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         return subLevels.stream().findFirst().orElseThrow();
      }
   }

   public static SubLevelArgumentType singleSubLevel() {
      return new SubLevelArgumentType(false, false);
   }

   public static SubLevelArgumentType subLevels() {
      return new SubLevelArgumentType(false, true);
   }

   public static SubLevelArgumentType subLevelsOrLevel() {
      return new SubLevelArgumentType(true, true);
   }

   @NotNull
   private static List<Pair<SubLevelSelectorModifierType, SubLevelSelectorModifierType.Modifier>> parseSelectorArguments(StringReader reader) throws CommandSyntaxException {
      List<Pair<SubLevelSelectorModifierType, SubLevelSelectorModifierType.Modifier>> modifiers = new ObjectArrayList();
      setSuggestions(reader, "[");
      List<Pair<String, Message>> permittedPreEntryToken = new ArrayList<>(
         SubLevelSelectorModifierType.getAllNamesWithTooltip().stream().map(s -> Pair.of((String)s.first() + "=", (Message)s.second())).toList()
      );
      permittedPreEntryToken.add(Pair.of("]", null));
      boolean isFirstEntry = true;
      if (reader.canRead() && reader.peek() == '[') {
         reader.skip();
         setSuggestionsWithTooltip(reader, permittedPreEntryToken);

         while (reader.canRead() && reader.peek() != ']') {
            if (reader.peek() == ',') {
               reader.skip();
            }

            setSuggestionsWithTooltip(reader, permittedPreEntryToken);
            String propertyName = readUntilEndOrCharacter(reader, '=');
            if (!reader.canRead() || reader.peek() != '=') {
               throw UNEXPECTED_END_OF_INPUT.createWithContext(reader);
            }

            reader.skip();
            SubLevelSelectorModifierType modifierType = SubLevelSelectorModifierType.getModifier(propertyName, reader);
            if (modifierType == null) {
               throw UNEXPECTED_END_OF_INPUT.createWithContext(reader);
            }

            SubLevelSelectorModifierType.Modifier modifier = modifierType.getParser().parse(reader);
            modifiers.add(Pair.of(modifierType, modifier));
            setSuggestionsWithTooltip(reader, permittedPreEntryToken);
            if (isFirstEntry) {
               permittedPreEntryToken.add(Pair.of(",", null));
               isFirstEntry = false;
            }
         }

         if (!reader.canRead() || reader.peek() != ']') {
            throw UNEXPECTED_END_OF_INPUT.createWithContext(reader);
         }

         reader.skip();
      }

      return modifiers;
   }

   public static void setSuggestions(StringReader reader, String... suggested) {
      setSuggestions(reader, Arrays.asList(suggested));
   }

   public static void setSuggestions(StringReader reader, List<String> suggested) {
      setSuggestionsWithTooltip(reader, suggested.stream().map(s -> Pair.of(s, (Message)null)).toList());
   }

   @SafeVarargs
   public static void setSuggestionsWithTooltip(StringReader reader, Pair<String, Message>... suggested) {
      setSuggestionsWithTooltip(reader, Arrays.asList(suggested));
   }

   public static void setSuggestionsWithTooltip(StringReader reader, List<Pair<String, Message>> suggested) {
      int cursor = reader.getCursor();
      suggestions = builder -> {
         SuggestionsBuilder nextSuggestion = builder.createOffset(cursor);

         for (Pair<String, Message> suggestion : suggested) {
            if (((String)suggestion.first()).startsWith(builder.getInput().substring(cursor))) {
               if (suggestion.second() != null) {
                  nextSuggestion.suggest((String)suggestion.first(), (Message)suggestion.second());
               } else {
                  nextSuggestion.suggest((String)suggestion.first());
               }
            }
         }

         return nextSuggestion.buildFuture();
      };
   }

   public static String readUntilEndOrCharacter(StringReader reader, char character) throws CommandSyntaxException {
      StringBuilder builder = new StringBuilder();

      while (reader.canRead() && reader.peek() != character) {
         builder.append(reader.read());
      }

      if (builder.isEmpty()) {
         throw UNEXPECTED_END_OF_INPUT.create();
      } else {
         return builder.toString();
      }
   }

   public SubLevelSelector parse(StringReader reader) throws CommandSyntaxException {
      ObjectList<Pair<String, Message>> allowedSelectors = new ObjectArrayList();
      if (this.allowStaticLevel) {
         allowedSelectors.add(Pair.of("static_world", Component.translatable("argument.sable.body.static_world")));
      }

      for (SubLevelSelectorType selector : SubLevelSelectorType.values()) {
         allowedSelectors.add(Pair.of("@" + selector.getChar(), selector.getTooltip()));
      }

      setSuggestionsWithTooltip(reader, allowedSelectors);
      if (this.allowStaticLevel && reader.canRead("static_world".length()) && reader.peek() == "static_world".charAt(0)) {
         String staticWorld = reader.readString();
         if (!staticWorld.equals("static_world")) {
            throw ERROR_INVALID_SUBLEVEL.create();
         } else {
            return new SubLevelSelector(null, new ObjectArrayList());
         }
      } else if (!reader.canRead()) {
         throw ERROR_INVALID_SUBLEVEL.create();
      } else {
         char firstChar = reader.read();
         if (!reader.canRead() || firstChar != '@') {
            throw ERROR_INVALID_SUBLEVEL.create();
         } else if (!reader.canRead()) {
            throw ERROR_INVALID_SUBLEVEL.create();
         } else {
            SubLevelSelectorType selectorType = SubLevelSelectorType.of(reader.read());
            if (selectorType == null) {
               throw ERROR_INVALID_SUBLEVEL.create();
            } else {
               int maximumResults = Integer.MAX_VALUE;
               if (selectorType.single()) {
                  maximumResults = 1;
               }

               List<Pair<SubLevelSelectorModifierType, SubLevelSelectorModifierType.Modifier>> modifiers = parseSelectorArguments(reader);

               for (Pair<SubLevelSelectorModifierType, SubLevelSelectorModifierType.Modifier> modifierPair : modifiers) {
                  maximumResults = Math.min(maximumResults, ((SubLevelSelectorModifierType.Modifier)modifierPair.second()).getMaxResults());
               }

               if (maximumResults > 1 && !this.allowMultiple) {
                  throw ERROR_SINGLE_SUB_LEVEL_REQUIRED.create();
               } else {
                  return new SubLevelSelector(selectorType, modifiers);
               }
            }
         }
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder builder) {
      StringReader stringreader = new StringReader(builder.getInput());
      stringreader.setCursor(builder.getStart());
      suggestions = NO_SUGGESTIONS;

      try {
         this.parse(stringreader);
      } catch (CommandSyntaxException var5) {
      }

      return suggestions.apply(builder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Info implements ArgumentTypeInfo<SubLevelArgumentType, SubLevelArgumentType.Info.Template> {
      private static final byte FLAG_MULTIPLE = 1;
      private static final byte FLAG_STATIC_ALLOWED = 2;

      public void serializeToNetwork(SubLevelArgumentType.Info.Template template, FriendlyByteBuf byteBuf) {
         int serialized = 0;
         if (template.allowMultiple) {
            serialized |= 1;
         }

         if (template.allowStaticLevel) {
            serialized |= 2;
         }

         byteBuf.writeByte(serialized);
      }

      public SubLevelArgumentType.Info.Template deserializeFromNetwork(FriendlyByteBuf arg) {
         byte serialized = arg.readByte();
         return new SubLevelArgumentType.Info.Template((serialized & 1) != 0, (serialized & 2) != 0);
      }

      public void serializeToJson(SubLevelArgumentType.Info.Template arg, JsonObject jsonObject) {
         jsonObject.addProperty("amount", arg.allowMultiple ? "single" : "multiple");
         jsonObject.addProperty("type", arg.allowStaticLevel ? "players" : "entities");
      }

      public SubLevelArgumentType.Info.Template unpack(SubLevelArgumentType arg) {
         return new SubLevelArgumentType.Info.Template(arg.allowMultiple, arg.allowStaticLevel);
      }

      public final class Template implements net.minecraft.commands.synchronization.ArgumentTypeInfo.Template<SubLevelArgumentType> {
         final boolean allowMultiple;
         final boolean allowStaticLevel;

         Template(final boolean allowMultiple, final boolean allowStaticLevel) {
            this.allowMultiple = allowMultiple;
            this.allowStaticLevel = allowStaticLevel;
         }

         public SubLevelArgumentType instantiate(CommandBuildContext commandBuildContext) {
            return new SubLevelArgumentType(this.allowStaticLevel, this.allowMultiple);
         }

         public ArgumentTypeInfo<SubLevelArgumentType, ?> type() {
            return Info.this;
         }
      }
   }
}
