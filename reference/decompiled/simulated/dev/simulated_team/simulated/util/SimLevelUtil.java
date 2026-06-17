package dev.simulated_team.simulated.util;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;

public class SimLevelUtil {
   public static boolean isAreaActuallyLoaded(Level level, BlockPos center, int range) {
      if (Sable.HELPER.getContaining(level, center) != null) {
         return true;
      } else if (!level.isAreaLoaded(center, range)) {
         return false;
      } else {
         if (level.isClientSide) {
            int minY = center.getY() - range;
            int maxY = center.getY() + range;
            if (maxY < level.getMinBuildHeight() || minY >= level.getMaxBuildHeight()) {
               return false;
            }

            int minX = center.getX() - range;
            int minZ = center.getZ() - range;
            int maxX = center.getX() + range;
            int maxZ = center.getZ() + range;
            int minChunkX = SectionPos.blockToSectionCoord(minX);
            int maxChunkX = SectionPos.blockToSectionCoord(maxX);
            int minChunkZ = SectionPos.blockToSectionCoord(minZ);
            int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
            ChunkSource chunkSource = level.getChunkSource();

            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
               for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                  if (!chunkSource.hasChunk(chunkX, chunkZ)) {
                     return false;
                  }
               }
            }
         }

         return true;
      }
   }
}
