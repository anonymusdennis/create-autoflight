package dev.ryanhcode.sable.sublevel.storage.region;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunk;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

public class SubLevelRegionFile extends SubLevelStorageFile {
   public static final String FILE_EXTENSION = ".slvlr";
   public static int SECTOR_SIZE = 128;
   public static int SIDE_LENGTH = 32;
   public static int LOG_SIDE_LENGTH = 5;

   public SubLevelRegionFile(Path path, Path externalFilePath) throws IOException {
      super(path, externalFilePath, SECTOR_SIZE);
   }

   public void trySave(int localX, int localZ, SubLevelHoldingChunk chunk) {
      CompoundTag tag = new CompoundTag();

      try {
         chunk.writeTo(tag);
         this.write(this.getIndex(localX, localZ), tag);
      } catch (IOException var6) {
         Sable.LOGGER.error("Failed to write sub-level holding chunk at ({}, {})", new Object[]{localX, localZ, var6});
      }
   }

   @Nullable
   public SubLevelHoldingChunk read(ChunkPos chunkPos) {
      int localX = chunkPos.getRegionLocalX();
      int localZ = chunkPos.getRegionLocalZ();

      try {
         CompoundTag tag = this.read(this.getIndex(localX, localZ));
         return tag == null ? null : SubLevelHoldingChunk.from(chunkPos, tag);
      } catch (IOException var5) {
         Sable.LOGGER.error("Failed to read sub-level holding chunk at ({}, {})", new Object[]{localX, localZ, var5});
         return null;
      }
   }

   public int getIndex(int localX, int localZ) {
      return localX | localZ << LOG_SIDE_LENGTH;
   }
}
