package com.simibubi.create.content.schematics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.content.schematics.table.SchematicTableBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.CreatePaths;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CSchematics;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ServerSchematicLoader {
   private final Map<String, ServerSchematicLoader.SchematicUploadEntry> activeUploads;
   private final ObjectArrayList<String> deadEntries = ObjectArrayList.of();

   public ServerSchematicLoader() {
      this.activeUploads = new HashMap<>();
   }

   public void tick() {
      int timeout = (Integer)this.getConfig().schematicIdleTimeout.get();

      for (String upload : this.activeUploads.keySet()) {
         ServerSchematicLoader.SchematicUploadEntry entry = this.activeUploads.get(upload);
         if (entry.idleTime++ > timeout) {
            Create.LOGGER.warn("Schematic Upload timed out: " + upload);
            this.deadEntries.add(upload);
         }
      }

      ObjectListIterator var5 = this.deadEntries.iterator();

      while (var5.hasNext()) {
         String toRemove = (String)var5.next();
         this.cancelUpload(toRemove);
      }

      this.deadEntries.clear();
   }

   public void shutdown() {
      new HashSet<>(this.activeUploads.keySet()).forEach(this::cancelUpload);
   }

   public void handleNewUpload(ServerPlayer player, String schematic, long size, BlockPos pos) {
      String playerName = player.getGameProfile().getName();
      Path baseDir = CreatePaths.UPLOADED_SCHEMATICS_DIR;
      Path playerPath = baseDir.resolve(playerName).normalize();
      Path uploadPath = playerPath.resolve(schematic).normalize();
      String playerSchematicId = playerName + "/" + schematic;
      if (playerPath.startsWith(baseDir) && uploadPath.startsWith(playerPath)) {
         FilesHelper.createFolderIfMissing(playerPath);
         if (!schematic.endsWith(".nbt")) {
            Create.LOGGER.warn("Attempted Schematic Upload with non-supported Format: {}", playerSchematicId);
         } else if (this.validateSchematicSizeOnServer(player, size)) {
            if (!this.activeUploads.containsKey(playerSchematicId)) {
               try {
                  SchematicTableBlockEntity table = this.getTable(player.getCommandSenderWorld(), pos);
                  if (table == null) {
                     return;
                  }

                  Files.deleteIfExists(uploadPath);

                  long count;
                  try (Stream<Path> list = Files.list(playerPath)) {
                     count = list.count();
                  }

                  if (count >= (long)((Integer)this.getConfig().maxSchematics.get()).intValue()) {
                     Stream<Path> list2 = Files.list(playerPath);
                     Optional<Path> lastFilePath = list2.filter(f -> !Files.isDirectory(f)).min(Comparator.comparingLong(f -> f.toFile().lastModified()));
                     list2.close();
                     if (lastFilePath.isPresent()) {
                        Files.deleteIfExists(lastFilePath.get());
                     }
                  }

                  OutputStream writer = Files.newOutputStream(uploadPath);
                  this.activeUploads.put(playerSchematicId, new ServerSchematicLoader.SchematicUploadEntry(writer, size, player.level(), pos));
                  table.startUpload(schematic);
               } catch (IOException var19) {
                  Create.LOGGER.error("Exception Thrown when starting Upload: {}", playerSchematicId, var19);
               }
            }
         }
      } else {
         Create.LOGGER.warn("Attempted Schematic Upload with path traversal: {}", playerSchematicId);
      }
   }

   protected boolean validateSchematicSizeOnServer(ServerPlayer player, long size) {
      long maxFileSize = (long)((Integer)this.getConfig().maxTotalSchematicSize.get()).intValue();
      if (size > maxFileSize * 1000L) {
         player.sendSystemMessage(CreateLang.translateDirect("schematics.uploadTooLarge").append(Component.literal(" (" + size / 1000L + " KB).")));
         player.sendSystemMessage(CreateLang.translateDirect("schematics.maxAllowedSize").append(Component.literal(" " + maxFileSize + " KB")));
         return false;
      } else {
         return true;
      }
   }

   public CSchematics getConfig() {
      return AllConfigs.server().schematics;
   }

   public void handleWriteRequest(ServerPlayer player, String schematic, byte[] data) {
      String playerSchematicId = player.getGameProfile().getName() + "/" + schematic;
      if (this.activeUploads.containsKey(playerSchematicId)) {
         ServerSchematicLoader.SchematicUploadEntry entry = this.activeUploads.get(playerSchematicId);
         entry.bytesUploaded += (long)data.length;
         if (data.length > (Integer)this.getConfig().maxSchematicPacketSize.get()) {
            Create.LOGGER.warn("Oversized Upload Packet received: {}", playerSchematicId);
            this.cancelUpload(playerSchematicId);
            return;
         }

         if (entry.bytesUploaded > entry.totalBytes) {
            Create.LOGGER.warn("Received more data than Expected: {}", playerSchematicId);
            this.cancelUpload(playerSchematicId);
            return;
         }

         try {
            entry.stream.write(data);
            entry.idleTime = 0;
            SchematicTableBlockEntity table = this.getTable(entry.world, entry.tablePos);
            if (table == null) {
               return;
            }

            table.uploadingProgress = (float)((double)entry.bytesUploaded / (double)entry.totalBytes);
            table.sendUpdate = true;
         } catch (IOException var7) {
            Create.LOGGER.error("Exception Thrown when uploading Schematic: {}", playerSchematicId, var7);
            this.cancelUpload(playerSchematicId);
         }
      }
   }

   protected void cancelUpload(String playerSchematicId) {
      if (this.activeUploads.containsKey(playerSchematicId)) {
         ServerSchematicLoader.SchematicUploadEntry entry = this.activeUploads.remove(playerSchematicId);

         try {
            entry.stream.close();
            Files.deleteIfExists(CreatePaths.UPLOADED_SCHEMATICS_DIR.resolve(playerSchematicId));
            Create.LOGGER.warn("Cancelled Schematic Upload: {}", playerSchematicId);
         } catch (IOException var5) {
            Create.LOGGER.error("Exception Thrown when cancelling Upload: {}", playerSchematicId, var5);
         }

         BlockPos pos = entry.tablePos;
         if (pos != null) {
            SchematicTableBlockEntity table = this.getTable(entry.world, pos);
            if (table != null) {
               table.finishUpload();
            }
         }
      }
   }

   public SchematicTableBlockEntity getTable(Level world, BlockPos pos) {
      BlockEntity be = world.getBlockEntity(pos);
      return be instanceof SchematicTableBlockEntity ? (SchematicTableBlockEntity)be : null;
   }

   public void handleFinishedUpload(ServerPlayer player, String schematic) {
      String playerSchematicId = player.getGameProfile().getName() + "/" + schematic;
      if (this.activeUploads.containsKey(playerSchematicId)) {
         try {
            this.activeUploads.get(playerSchematicId).stream.close();
            ServerSchematicLoader.SchematicUploadEntry removed = this.activeUploads.remove(playerSchematicId);
            Level world = removed.world;
            BlockPos pos = removed.tablePos;
            Create.LOGGER.info("New Schematic Uploaded: " + playerSchematicId);
            if (pos == null) {
               return;
            }

            BlockState blockState = world.getBlockState(pos);
            if (AllBlocks.SCHEMATIC_TABLE.get() != blockState.getBlock()) {
               return;
            }

            SchematicTableBlockEntity table = this.getTable(world, pos);
            if (table == null) {
               return;
            }

            table.finishUpload();
            table.inventory.setStackInSlot(1, SchematicItem.create(world, schematic, player.getGameProfile().getName()));
         } catch (IOException var9) {
            Create.LOGGER.error("Exception Thrown when finishing Upload: {}", playerSchematicId, var9);
         }
      }
   }

   public void handleInstantSchematic(ServerPlayer player, String schematic, Level world, BlockPos pos, BlockPos bounds) {
      String playerName = player.getGameProfile().getName();
      Path baseDir = CreatePaths.UPLOADED_SCHEMATICS_DIR;
      Path playerPath = baseDir.resolve(playerName).normalize();
      Path uploadPath = playerPath.resolve(schematic).normalize();
      String playerSchematicId = playerName + "/" + schematic;
      if (playerPath.startsWith(baseDir) && uploadPath.startsWith(playerPath)) {
         FilesHelper.createFolderIfMissing(playerPath);
         if (!schematic.endsWith(".nbt")) {
            Create.LOGGER.warn("Attempted Schematic Upload with non-supported Format: {}", playerSchematicId);
         } else if (AllItems.SCHEMATIC_AND_QUILL.isIn(player.getMainHandItem())) {
            if (this.tryDeleteOldestSchematic(playerPath)) {
               SchematicExport.SchematicExportResult result = SchematicExport.saveSchematic(
                  playerPath, schematic, true, world, pos, pos.offset(bounds).offset(-1, -1, -1)
               );
               if (result != null) {
                  player.setItemInHand(InteractionHand.MAIN_HAND, SchematicItem.create(world, schematic, playerName));
               } else {
                  CreateLang.translate("schematicAndQuill.instant_failed").style(ChatFormatting.RED).sendStatus(player);
               }
            }
         }
      } else {
         Create.LOGGER.warn("Attempted Schematic Upload with path traversal: {}", playerSchematicId);
      }
   }

   private boolean tryDeleteOldestSchematic(Path dir) {
      try {
         boolean var5;
         try (Stream<Path> stream = Files.list(dir)) {
            List<Path> files = stream.toList();
            if (files.size() < (Integer)this.getConfig().maxSchematics.get()) {
               return true;
            }

            Optional<Path> oldest = files.stream().min(Comparator.comparingLong(this::getLastModifiedTime));
            Files.delete(oldest.orElseThrow());
            var5 = true;
         }

         return var5;
      } catch (IllegalStateException | IOException var8) {
         Create.LOGGER.error("Error deleting oldest schematic", var8);
         return false;
      }
   }

   private long getLastModifiedTime(Path file) {
      try {
         return Files.getLastModifiedTime(file).toMillis();
      } catch (IOException var3) {
         Create.LOGGER.error("Error getting modification time of file {}", file.getFileName(), var3);
         throw new IllegalStateException(var3);
      }
   }

   public static class SchematicUploadEntry {
      public Level world;
      public BlockPos tablePos;
      public OutputStream stream;
      public long bytesUploaded;
      public long totalBytes;
      public int idleTime;

      public SchematicUploadEntry(OutputStream stream, long totalBytes, Level world, BlockPos tablePos) {
         this.stream = stream;
         this.totalBytes = totalBytes;
         this.tablePos = tablePos;
         this.world = world;
         this.bytesUploaded = 0L;
         this.idleTime = 0;
      }
   }
}
