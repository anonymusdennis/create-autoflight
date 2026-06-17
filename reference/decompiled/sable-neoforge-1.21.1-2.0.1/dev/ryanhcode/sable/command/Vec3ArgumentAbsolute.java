package dev.ryanhcode.sable.command;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.world.phys.Vec3;

public class Vec3ArgumentAbsolute implements ArgumentType<Vec3> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0");

   public static Vec3ArgumentAbsolute vec3() {
      return new Vec3ArgumentAbsolute();
   }

   public Vec3 parse(StringReader stringReader) throws CommandSyntaxException {
      int i = stringReader.getCursor();
      double worldCoordinate = this.parseDouble(stringReader);
      if (stringReader.canRead() && stringReader.peek() == ' ') {
         stringReader.skip();
         double worldCoordinate2 = this.parseDouble(stringReader);
         if (stringReader.canRead() && stringReader.peek() == ' ') {
            stringReader.skip();
            double worldCoordinate3 = this.parseDouble(stringReader);
            return new Vec3(worldCoordinate, worldCoordinate2, worldCoordinate3);
         } else {
            stringReader.setCursor(i);
            throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(stringReader);
         }
      } else {
         stringReader.setCursor(i);
         throw Vec3Argument.ERROR_NOT_COMPLETE.createWithContext(stringReader);
      }
   }

   private double parseDouble(StringReader stringReader) throws CommandSyntaxException {
      if (!stringReader.canRead()) {
         throw WorldCoordinate.ERROR_EXPECTED_DOUBLE.createWithContext(stringReader);
      } else {
         int i = stringReader.getCursor();
         double d = stringReader.canRead() && stringReader.peek() != ' ' ? stringReader.readDouble() : 0.0;
         String string = stringReader.getString().substring(i, stringReader.getCursor());
         return string.isEmpty() ? 0.0 : d;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
      if (!(commandContext.getSource() instanceof SharedSuggestionProvider)) {
         return Suggestions.empty();
      } else {
         String string = suggestionsBuilder.getRemaining();
         List<String> list = Lists.newArrayList();
         Predicate<String> predicate = Commands.createValidator(this::parse);
         String[] strings = Strings.isNullOrEmpty(string) ? new String[0] : string.split(" ");

         for (int i = 3; i > strings.length; i--) {
            StringBuilder s = new StringBuilder();

            for (int j = 0; j < i; j++) {
               s.append(j < strings.length ? strings[j] : "0");
               if (j < i - 1) {
                  s.append(" ");
               }
            }

            if (!predicate.test(s.toString()) && i == 3) {
               break;
            }

            list.add(s.toString());
         }

         return SharedSuggestionProvider.suggest(list, suggestionsBuilder);
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
