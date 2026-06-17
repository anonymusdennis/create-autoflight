package com.simibubi.create.infrastructure.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.client.SchematicAndQuillHandler;
import com.simibubi.create.foundation.utility.CreatePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class CreateTestCommand {
   private static final Path gametests = CreatePaths.GAME_DIR.getParent().resolve("src/main/resources/data/create/structure/gametest").toAbsolutePath();

   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return Commands.literal("test")
         .then(
            Commands.literal("export")
               .then(
                  Commands.argument("path", StringArgumentType.greedyString())
                     .suggests(CreateTestCommand::getSuggestions)
                     .executes(
                        ctx -> handleExport(
                              (CommandSourceStack)ctx.getSource(), ((CommandSourceStack)ctx.getSource()).getLevel(), StringArgumentType.getString(ctx, "path")
                           )
                     )
               )
         );
   }

   private static int handleExport(CommandSourceStack source, ServerLevel level, String path) {
      SchematicAndQuillHandler handler = CreateClient.SCHEMATIC_AND_QUILL_HANDLER;
      if (handler.firstPos != null && handler.secondPos != null) {
         SchematicExport.SchematicExportResult result = SchematicExport.saveSchematic(gametests, path, true, level, handler.firstPos, handler.secondPos);
         if (result == null) {
            source.sendFailure(Component.literal("Failed to export, check logs").withStyle(ChatFormatting.RED));
         } else {
            sendSuccess(source, "Successfully exported test!", ChatFormatting.GREEN);
            sendSuccess(source, "Overwritten: " + result.overwritten(), ChatFormatting.AQUA);
            sendSuccess(source, "File: " + result.file(), ChatFormatting.GRAY);
         }

         return 0;
      } else {
         source.sendFailure(Component.literal("You must select an area with the Schematic and Quill first."));
         return 0;
      }
   }

   private static void sendSuccess(CommandSourceStack source, String text, ChatFormatting color) {
      source.sendSuccess(() -> Component.literal(text).withStyle(color), true);
   }

   private static CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
      String path = builder.getRemaining();
      if (path.contains("/") && !path.contains("..")) {
         int lastSlash = path.lastIndexOf("/");
         Path subDir = gametests.resolve(path.substring(0, lastSlash));
         if (Files.exists(subDir)) {
            findInDir(subDir, builder);
         }

         return builder.buildFuture();
      } else {
         return findInDir(gametests, builder);
      }
   }

   private static CompletableFuture<Suggestions> findInDir(Path dir, SuggestionsBuilder builder) {
      try (Stream<Path> paths = Files.list(dir)) {
         paths.filter(p -> Files.isDirectory(p) || p.toString().endsWith(".nbt")).forEach(path -> {
            String file = path.toString().replaceAll("\\\\", "/").substring(gametests.toString().length() + 1);
            if (Files.isDirectory(path)) {
               file = file + "/";
            }

            builder.suggest(file);
         });
      } catch (IOException var7) {
         throw new RuntimeException(var7);
      }

      return builder.buildFuture();
   }
}
