package dev.ryanhcode.sable.sublevel.storage.serialization;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelRegionFile;
import dev.ryanhcode.sable.sublevel.storage.region.SubLevelStorageFile;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.FileUtil;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Internal;

public class SubLevelStorage implements AutoCloseable {
   public static int MAX_CACHE_SIZE = 128;
   private final Long2ObjectLinkedOpenHashMap<SubLevelRegionFile> regionCache = new Long2ObjectLinkedOpenHashMap();
   private final Long2ObjectLinkedOpenHashMap<SubLevelStorageFile> storageCache = new Long2ObjectLinkedOpenHashMap();
   private final Path folder;

   public SubLevelStorage(Path folder) {
      this.folder = folder;
   }

   @NotNull
   private static String getFileName(ChunkPos chunkPos) {
      return "r." + chunkPos.getRegionX() + "." + chunkPos.getRegionZ();
   }

   @NotNull
   private static String getFileName(ChunkPos chunkPos, int index) {
      return "r." + chunkPos.getRegionX() + "." + chunkPos.getRegionZ() + "." + index;
   }

   private SubLevelRegionFile getRegionFile(ChunkPos chunkPos) throws IOException {
      long longKey = ChunkPos.asLong(chunkPos.getRegionX(), chunkPos.getRegionZ());
      SubLevelRegionFile existingFile = (SubLevelRegionFile)this.regionCache.getAndMoveToFirst(longKey);
      if (existingFile != null) {
         return existingFile;
      } else {
         if (this.regionCache.size() >= MAX_CACHE_SIZE) {
            ((SubLevelRegionFile)this.regionCache.removeLast()).close();
         }

         Path path = this.getPath(chunkPos);
         Path externalPath = this.getExternalPath(chunkPos);
         SubLevelRegionFile loadedRegion = new SubLevelRegionFile(path, externalPath);
         this.regionCache.putAndMoveToFirst(longKey, loadedRegion);
         return loadedRegion;
      }
   }

   private SubLevelStorageFile getRegionStorageFile(ChunkPos chunkPos, int index) throws IOException {
      long longKey = SectionPos.asLong(chunkPos.getRegionX(), index, chunkPos.getRegionZ());
      SubLevelStorageFile existingFile = (SubLevelStorageFile)this.storageCache.getAndMoveToFirst(longKey);
      if (existingFile != null) {
         return existingFile;
      } else {
         if (this.storageCache.size() >= MAX_CACHE_SIZE) {
            ((SubLevelStorageFile)this.storageCache.removeLast()).close();
         }

         FileUtil.createDirectoriesSafe(this.folder);
         Path path = this.getPath(chunkPos, index);
         Path externalPath = this.getExternalPath(chunkPos);
         FileUtil.createDirectoriesSafe(externalPath);
         SubLevelStorageFile loadedRegion = new SubLevelStorageFile(path, externalPath);
         this.storageCache.putAndMoveToFirst(longKey, loadedRegion);
         return loadedRegion;
      }
   }

   public SubLevelHoldingChunk attemptLoadHoldingChunk(ChunkPos chunkPos) {
      try {
         SubLevelRegionFile regionFile = this.getRegionFile(chunkPos);
         return regionFile.read(chunkPos);
      } catch (IOException var3) {
         Sable.LOGGER.error("Failed to load holding chunk for {}", chunkPos, var3);
         return null;
      }
   }

   public void attemptSaveHoldingChunk(ChunkPos chunkPos, SubLevelHoldingChunk holdingChunk) {
      try {
         SubLevelRegionFile regionFile = this.getRegionFile(chunkPos);
         regionFile.trySave(chunkPos.getRegionLocalX(), chunkPos.getRegionLocalZ(), holdingChunk);
      } catch (IOException var4) {
         Sable.LOGGER.error("Failed to save holding chunk for {}", chunkPos, var4);
      }
   }

   public SubLevelData attemptLoadSubLevel(ChunkPos chunkPos, SavedSubLevelPointer pointer) {
      try {
         SubLevelStorageFile storageFile = this.getRegionStorageFile(chunkPos, pointer.storageIndex());
         CompoundTag tag = storageFile.read(pointer.subLevelIndex());
         if (tag == null) {
            Sable.LOGGER.error("Couldn't find sub-level at index {} in storage file for chunk {}", pointer.subLevelIndex(), chunkPos);
            return null;
         } else {
            SubLevelData subLevel = SubLevelSerializer.fromData(tag);
            if (subLevel != null) {
               subLevel.setOriginLoadedChunk(chunkPos);
            } else {
               Sable.LOGGER.error("Failed to load sub-level at index {} in storage file for chunk {}", pointer.subLevelIndex(), chunkPos);
            }

            return subLevel;
         }
      } catch (IOException var6) {
         Sable.LOGGER.error("Failed to load sub-level for {}", chunkPos, var6);
         return null;
      }
   }

   public GlobalSavedSubLevelPointer attemptSaveSubLevel(ChunkPos chunkPos, SubLevelData subLevel) {
      try {
         int storageIndex = 0;

         while (true) {
            SubLevelStorageFile storageFile = this.getRegionStorageFile(chunkPos, storageIndex);
            int subLevelIndex = storageFile.findFreeIndex();
            if (subLevelIndex != -1 && subLevelIndex < storageFile.getTotalIndexCapacity()) {
               storageFile.write(subLevelIndex, subLevel.fullTag());
               return new GlobalSavedSubLevelPointer(chunkPos, (short)storageIndex, (short)subLevelIndex);
            }

            storageIndex++;
         }
      } catch (IOException var6) {
         Sable.LOGGER.error("Failed to save sub-level for {}", chunkPos, var6);
         return null;
      }
   }

   public void attemptSaveSubLevel(GlobalSavedSubLevelPointer pointer, SubLevelData subLevel) {
      try {
         SubLevelStorageFile storageFile = this.getRegionStorageFile(pointer.chunkPos(), pointer.storageIndex());
         storageFile.write(pointer.subLevelIndex(), subLevel != null ? subLevel.fullTag() : null);
      } catch (IOException var4) {
         Sable.LOGGER.error("Failed to save sub-level for {}", pointer.chunkPos(), var4);
      }
   }

   @NotNull
   private Path getExternalPath(ChunkPos chunkPos) {
      return this.folder.resolve(getFileName(chunkPos) + ".r");
   }

   @NotNull
   private Path getExternalPath(ChunkPos chunkPos, int index) {
      return this.folder.resolve(getFileName(chunkPos, index) + ".s");
   }

   @NotNull
   private Path getPath(ChunkPos chunkPos) {
      return this.folder.resolve(getFileName(chunkPos) + ".slvlr");
   }

   @NotNull
   private Path getPath(ChunkPos chunkPos, int index) {
      return this.folder.resolve(getFileName(chunkPos, index) + ".slvls");
   }

   @Override
   public void close() throws IOException {
      ExceptionCollector<IOException> exceptionCollector = new ExceptionCollector();
      ObjectIterator var2 = this.storageCache.values().iterator();

      while (var2.hasNext()) {
         SubLevelStorageFile storageFile = (SubLevelStorageFile)var2.next();

         try {
            storageFile.close();
         } catch (IOException var6) {
            exceptionCollector.add(var6);
         }
      }

      var2 = this.regionCache.values().iterator();

      while (var2.hasNext()) {
         SubLevelRegionFile regionFile = (SubLevelRegionFile)var2.next();

         try {
            regionFile.close();
         } catch (IOException var5) {
            exceptionCollector.add(var5);
         }
      }

      exceptionCollector.throwIfPresent();
   }

   @NotNull
   @Internal
   public Path getFolder() {
      return this.folder;
   }

   public void flush() throws IOException {
      ObjectIterator var1 = this.regionCache.values().iterator();

      while (var1.hasNext()) {
         SubLevelRegionFile regionFile = (SubLevelRegionFile)var1.next();
         regionFile.flush();
      }

      var1 = this.storageCache.values().iterator();

      while (var1.hasNext()) {
         SubLevelStorageFile regionFile = (SubLevelStorageFile)var1.next();
         regionFile.flush();
      }
   }
}
