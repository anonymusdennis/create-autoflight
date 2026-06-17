package dev.ryanhcode.sable.sublevel.plot;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ChunkHolder.LevelChangeListener;
import net.minecraft.server.level.ChunkHolder.PlayerProvider;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlotChunkHolder extends ChunkHolder {
   private final LevelChunk chunk;
   private final HeatDataChunkSection[] heatSections;
   @Nullable
   private BoundingBox3i boundingBox;

   public PlotChunkHolder(
      LevelChunk chunk,
      ChunkPos pos,
      LevelHeightAccessor levelHeightAccessor,
      LevelLightEngine levelLightEngine,
      LevelChangeListener levelChangeListener,
      PlayerProvider playerProvider
   ) {
      super(pos, 31, levelHeightAccessor, levelLightEngine, levelChangeListener, playerProvider);
      if (chunk == null) {
         throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Chunk not found in plot container"));
      } else {
         this.chunk = chunk;
         this.heatSections = new HeatDataChunkSection[chunk.getSectionsCount()];
         this.tickingChunkFuture = CompletableFuture.completedFuture(ChunkResult.of(chunk));
         this.entityTickingChunkFuture = CompletableFuture.completedFuture(ChunkResult.of(chunk));
         this.fullChunkFuture = CompletableFuture.completedFuture(ChunkResult.of(chunk));
         if (!this.chunk.isEmpty()) {
            this.buildBoundingBox();
         }
      }
   }

   public static PlotChunkHolder create(Level level, ChunkPos pos, LevelLightEngine lightEngine, LevelChunk chunk) {
      ChunkMap chunkMap = null;
      if (level.getChunkSource() instanceof ServerChunkCache chunkCache) {
         chunkMap = chunkCache.chunkMap;
      }

      return new PlotChunkHolder(chunk, pos, level, lightEngine, null, chunkMap);
   }

   protected void buildBoundingBox() {
      this.boundingBox = null;
      LevelChunkSection[] sections = this.chunk.getSections();

      for (int i = 0; i < sections.length; i++) {
         LevelChunkSection section = sections[i];
         int sectionMinY = this.chunk.getSectionYFromSectionIndex(i) << 4;
         if (section != null && !section.hasOnlyAir()) {
            for (int x = 0; x < 16; x++) {
               for (int y = 0; y < 16; y++) {
                  for (int z = 0; z < 16; z++) {
                     if (!section.getBlockState(x, y, z).isAir()) {
                        if (this.boundingBox == null) {
                           this.boundingBox = new BoundingBox3i(x, y + sectionMinY, z, x, y + sectionMinY, z);
                        } else {
                           this.boundingBox = this.boundingBox.expandTo(x, y + sectionMinY, z, this.boundingBox);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void handleBlockChange(int x, int y, int z, BlockState oldState, BlockState newState) {
      if (!this.chunk.getLevel().isClientSide) {
         if (oldState.isAir() && !newState.isAir()) {
            if (this.boundingBox == null) {
               this.boundingBox = new BoundingBox3i(x, y, z, x, y, z);
            } else {
               this.boundingBox = this.boundingBox.expandTo(x, y, z, this.boundingBox);
            }
         } else if (!oldState.isAir()
            && newState.isAir()
            && this.boundingBox != null
            && (
               this.boundingBox.minX == x
                  || this.boundingBox.maxX == x
                  || this.boundingBox.minY == y
                  || this.boundingBox.maxY == y
                  || this.boundingBox.minZ == z
                  || this.boundingBox.maxZ == z
            )) {
            this.buildBoundingBox();
         }
      }
   }

   public void blockChanged(BlockPos blockPos) {
      super.blockChanged(blockPos);
   }

   protected void updateFutures(ChunkMap chunkMap, Executor executor) {
   }

   public boolean isReadyForSaving() {
      return false;
   }

   public LevelChunk getChunk() {
      return this.chunk;
   }

   public BoundingBox3ic getBoundingBox() {
      return this.boundingBox;
   }

   public void rescheduleChunkTask(ChunkMap chunkMap, @Nullable ChunkStatus chunkStatus) {
   }

   public boolean wasAccessibleSinceLastSave() {
      return false;
   }

   @NotNull
   public LevelChunk getTickingChunk() {
      return this.chunk;
   }

   @Nullable
   public HeatDataChunkSection getHeatSection(int y) {
      int index = y - this.chunk.getMinSection();
      return index >= 0 && index < this.heatSections.length ? this.heatSections[index] : null;
   }

   public void setHeatSection(int y, HeatDataChunkSection section) {
      int index = y - this.chunk.getMinSection();
      if (index >= 0 && index < this.heatSections.length) {
         this.heatSections[index] = section;
      }
   }
}
