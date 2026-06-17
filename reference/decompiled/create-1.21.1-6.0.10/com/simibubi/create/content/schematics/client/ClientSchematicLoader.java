package com.simibubi.create.content.schematics.client;

import com.simibubi.create.Create;
import com.simibubi.create.content.schematics.packet.SchematicUploadPacket;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.CreatePaths;
import com.simibubi.create.foundation.utility.FilesHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientSchematicLoader {
   public static final int PACKET_DELAY = 10;
   private final List<Component> availableSchematics = new ArrayList<>();
   private final Map<String, InputStream> activeUploads = new HashMap<>();
   private int packetCycle;

   public ClientSchematicLoader() {
      this.refresh();
   }

   public void tick() {
      if (!this.activeUploads.isEmpty()) {
         if (this.packetCycle-- <= 0) {
            this.packetCycle = 10;

            for (String schematic : new HashSet<>(this.activeUploads.keySet())) {
               this.continueUpload(schematic);
            }
         }
      }
   }

   public void startNewUpload(String schematic) {
      Path path = CreatePaths.SCHEMATICS_DIR.resolve(schematic);
      if (!Files.exists(path)) {
         Create.LOGGER.error("Missing Schematic file: {}", path);
      } else {
         try {
            long size = Files.size(path);
            if (!validateSizeLimitation(size)) {
               return;
            }

            if (!isGZIPEncoded(path.toFile())) {
               LocalPlayer player = Minecraft.getInstance().player;
               if (player != null) {
                  player.displayClientMessage(CreateLang.translateDirect("schematics.wrongFormat"), false);
               }

               return;
            }

            InputStream in = Files.newInputStream(path, StandardOpenOption.READ);
            this.activeUploads.put(schematic, in);
            CatnipServices.NETWORK.sendToServer(SchematicUploadPacket.begin(schematic, size));
         } catch (IOException var7) {
            Create.LOGGER.error("Encountered an error while starting schematic upload", var7);
         }
      }
   }

   public static boolean validateSizeLimitation(long size) {
      if (Minecraft.getInstance().hasSingleplayerServer()) {
         return true;
      } else {
         long maxSize = (long)((Integer)AllConfigs.server().schematics.maxTotalSchematicSize.get()).intValue();
         if (size > maxSize * 1000L) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
               player.displayClientMessage(CreateLang.translateDirect("schematics.uploadTooLarge").append(" (" + size / 1000L + " KB)."), false);
               player.displayClientMessage(CreateLang.translateDirect("schematics.maxAllowedSize").append(" " + maxSize + " KB"), false);
            }

            return false;
         } else {
            return true;
         }
      }
   }

   public static boolean isGZIPEncoded(File file) {
      try {
         boolean var5;
         try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[2];
            if (fis.read(bytes) != 2) {
               return false;
            }

            int byte1 = bytes[0] & 255;
            int byte2 = bytes[1] & 255;
            var5 = byte1 == 31 && byte2 == 139;
         }

         return var5;
      } catch (IOException var8) {
         return false;
      }
   }

   private void continueUpload(String schematic) {
      if (this.activeUploads.containsKey(schematic)) {
         int maxPacketSize = (Integer)AllConfigs.server().schematics.maxSchematicPacketSize.get();
         byte[] data = new byte[maxPacketSize];

         try {
            int status = this.activeUploads.get(schematic).read(data);
            if (status != -1) {
               if (status < maxPacketSize) {
                  data = Arrays.copyOf(data, status);
               }

               if (Minecraft.getInstance().level == null) {
                  this.activeUploads.remove(schematic);
                  return;
               }

               CatnipServices.NETWORK.sendToServer(SchematicUploadPacket.write(schematic, data));
            }

            if (status < maxPacketSize) {
               this.finishUpload(schematic);
            }
         } catch (IOException var5) {
            Create.LOGGER.error("Encountered a error while uploading schematic", var5);
         }
      }
   }

   private void finishUpload(String schematic) {
      if (this.activeUploads.containsKey(schematic)) {
         CatnipServices.NETWORK.sendToServer(SchematicUploadPacket.finish(schematic));
         this.activeUploads.remove(schematic);
      }
   }

   public void refresh() {
      FilesHelper.createFolderIfMissing(CreatePaths.SCHEMATICS_DIR);
      this.availableSchematics.clear();

      try (Stream<Path> paths = Files.list(CreatePaths.SCHEMATICS_DIR)) {
         paths.filter(f -> !Files.isDirectory(f) && f.getFileName().toString().endsWith(".nbt")).forEach(path -> {
            if (!Files.isDirectory(path)) {
               this.availableSchematics.add(Component.literal(path.getFileName().toString()));
            }
         });
      } catch (NoSuchFileException var6) {
      } catch (IOException var7) {
         Create.LOGGER.error("Failed to refresh schematics", var7);
      }

      this.availableSchematics.sort((aT, bT) -> {
         String a = aT.getString();
         String b = bT.getString();
         if (a.endsWith(".nbt")) {
            a = a.substring(0, a.length() - 4);
         }

         if (b.endsWith(".nbt")) {
            b = b.substring(0, b.length() - 4);
         }

         int aLength = a.length();
         int bLength = b.length();
         int minSize = Math.min(aLength, bLength);
         boolean asNumeric = false;
         int lastNumericCompare = 0;

         for (int i = 0; i < minSize; i++) {
            char aChar = a.charAt(i);
            char bChar = b.charAt(i);
            boolean aNumber = aChar >= '0' && aChar <= '9';
            boolean bNumber = bChar >= '0' && bChar <= '9';
            if (asNumeric) {
               if (aNumber && bNumber) {
                  if (lastNumericCompare == 0) {
                     lastNumericCompare = aChar - bChar;
                  }
               } else {
                  if (aNumber) {
                     return 1;
                  }

                  if (bNumber) {
                     return -1;
                  }

                  if (lastNumericCompare != 0) {
                     return lastNumericCompare;
                  }

                  if (aChar != bChar) {
                     return aChar - bChar;
                  }

                  asNumeric = false;
               }
            } else if (aNumber && bNumber) {
               asNumeric = true;
               if (lastNumericCompare == 0) {
                  lastNumericCompare = aChar - bChar;
               }
            } else if (aChar != bChar) {
               return aChar - bChar;
            }
         }

         if (!asNumeric) {
            return aLength - bLength;
         } else if (aLength > bLength && a.charAt(bLength) >= '0' && a.charAt(bLength) <= '9') {
            return 1;
         } else if (bLength > aLength && b.charAt(aLength) >= '0' && b.charAt(aLength) <= '9') {
            return -1;
         } else {
            return lastNumericCompare == 0 ? aLength - bLength : lastNumericCompare;
         }
      });
   }

   public List<Component> getAvailableSchematics() {
      return this.availableSchematics;
   }
}
