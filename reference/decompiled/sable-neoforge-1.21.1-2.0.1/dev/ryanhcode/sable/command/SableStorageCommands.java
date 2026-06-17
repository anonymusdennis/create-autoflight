package dev.ryanhcode.sable.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelStorage;
import java.io.File;
import java.util.Formatter;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.Action;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SableStorageCommands {
   public static void register(LiteralArgumentBuilder<CommandSourceStack> sableBuilder, CommandBuildContext buildContext) {
      sableBuilder.then(((LiteralArgumentBuilder)Commands.literal("storage").then(Commands.literal("find_all_sub_levels").executes(ctx -> {
         ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
         ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);
         SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();
         SubLevelStorage storage = holdingChunkMap.getStorage();
         CommandSourceStack source = (CommandSourceStack)ctx.getSource();
         File[] regionFiles = storage.getFolder().toFile().listFiles((dir, name) -> name.endsWith(".slvlr"));
         if (regionFiles != null) {
            for (File regionFile : regionFiles) {
               String fileName = regionFile.getName();
               String withoutExtension = fileName.substring(0, fileName.length() - ".slvlr".length());
               String[] parts = withoutExtension.split("\\.");
               if (parts.length == 3) {
                  int regionX;
                  int regionZ;
                  try {
                     regionX = Integer.parseInt(parts[1]);
                     regionZ = Integer.parseInt(parts[2]);
                  } catch (NumberFormatException var23) {
                     continue;
                  }

                  for (int localX = 0; localX < SubLevelRegionFile.SIDE_LENGTH; localX++) {
                     for (int localZ = 0; localZ < SubLevelRegionFile.SIDE_LENGTH; localZ++) {
                        ChunkPos chunkPos = new ChunkPos(regionX * SubLevelRegionFile.SIDE_LENGTH + localX, regionZ * SubLevelRegionFile.SIDE_LENGTH + localZ);
                        SubLevelHoldingChunk holdingChunk = storage.attemptLoadHoldingChunk(chunkPos);
                        if (holdingChunk != null) {
                           for (SavedSubLevelPointer pointer : holdingChunk.getSubLevelPointers()) {
                              SubLevelData data = storage.attemptLoadSubLevel(chunkPos, pointer);
                              logFoundSubLevel(pointer, data, chunkPos, source, level);
                           }
                        }
                     }
                  }
               }
            }
         }

         return 1;
      }))).then(Commands.literal("find").then(Commands.argument("name", StringArgumentType.string()).executes(ctx -> {
         ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
         ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(level);
         SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();
         SubLevelStorage storage = holdingChunkMap.getStorage();
         CommandSourceStack source = (CommandSourceStack)ctx.getSource();
         String nameArgument = StringArgumentType.getString(ctx, "name");
         File[] regionFiles = storage.getFolder().toFile().listFiles((dir, namex) -> namex.endsWith(".slvlr"));
         if (regionFiles != null) {
            for (File regionFile : regionFiles) {
               String fileName = regionFile.getName();
               String withoutExtension = fileName.substring(0, fileName.length() - ".slvlr".length());
               String[] parts = withoutExtension.split("\\.");
               if (parts.length == 3) {
                  int regionX;
                  int regionZ;
                  try {
                     regionX = Integer.parseInt(parts[1]);
                     regionZ = Integer.parseInt(parts[2]);
                  } catch (NumberFormatException var25) {
                     continue;
                  }

                  for (int localX = 0; localX < SubLevelRegionFile.SIDE_LENGTH; localX++) {
                     for (int localZ = 0; localZ < SubLevelRegionFile.SIDE_LENGTH; localZ++) {
                        ChunkPos chunkPos = new ChunkPos(regionX * SubLevelRegionFile.SIDE_LENGTH + localX, regionZ * SubLevelRegionFile.SIDE_LENGTH + localZ);
                        SubLevelHoldingChunk holdingChunk = storage.attemptLoadHoldingChunk(chunkPos);
                        if (holdingChunk != null) {
                           for (SavedSubLevelPointer pointer : holdingChunk.getSubLevelPointers()) {
                              SubLevelData data = storage.attemptLoadSubLevel(chunkPos, pointer);
                              String name = data.fullTag().contains("display_name") ? data.fullTag().getString("display_name") : data.uuid().toString();
                              if (name != null && name.equals(nameArgument)) {
                                 logFoundSubLevel(pointer, data, chunkPos, source, level);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return 1;
      }))));
   }

   private static void logFoundSubLevel(SavedSubLevelPointer pointer, SubLevelData data, ChunkPos chunkPos, CommandSourceStack source, ServerLevel level) {
      if (data != null) {
         String name = data.fullTag().contains("display_name") ? data.fullTag().getString("display_name") : data.uuid().toString();
         GlobalSavedSubLevelPointer globalPointer = new GlobalSavedSubLevelPointer(chunkPos, pointer.storageIndex(), pointer.subLevelIndex());
         Pose3d pose = data.pose();
         source.sendSuccess(
            () -> {
               Vector3dc pos = pose.position();
               MutableComponent component = Component.translatable("commands.sable.info.name", new Object[]{Component.literal(name)});
               ResourceLocation dimension = level.dimension().location();
               Component fileId = Component.translatable("commands.sable.info.name.tooltip", new Object[]{globalPointer.toString()});
               component.setStyle(
                  Style.EMPTY
                     .withClickEvent(
                        new ClickEvent(
                           Action.SUGGEST_COMMAND,
                           new Formatter().format(Locale.ROOT, "/execute in %s run tp @s %.2f %.2f %.2f", dimension, pos.x(), pos.y(), pos.z()).toString()
                        )
                     )
                     .withHoverEvent(new HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, fileId))
                     .withColor(ChatFormatting.GRAY)
               );
               return component;
            },
            false
         );
         source.sendSuccess(() -> {
            Vector3dc pos = pose.position();
            return Component.translatable("commands.sable.info.position", new Object[]{pos.x(), pos.y(), pos.z()});
         }, false);
         source.sendSuccess(() -> {
            Vector3d size = data.bounds().size();
            return Component.translatable("commands.sable.info.world_bounds", new Object[]{size.x, size.y, size.z});
         }, false);
      }
   }
}
