package dev.ryanhcode.sable.util;

import dev.ryanhcode.sable.mixin.level_accelerator.ServerChunkCacheAccessor;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LevelAccelerator implements BlockGetter {
   public static final boolean USE_CACHE_MAP = false;
   private final Level level;
   private long cachedChunkPos = 0L;
   private LevelChunk cachedChunkObj = null;
   private final int minBuildHeight;
   private final int minSection;
   private final int maxBuildHeight;
   private final Long2ObjectMap<LevelChunk> cachedLevelChunks = new Long2ObjectOpenHashMap();

   public LevelAccelerator(Level level) {
      this.level = level;
      this.minBuildHeight = level.getMinBuildHeight();
      this.maxBuildHeight = level.getMaxBuildHeight();
      this.minSection = level.getMinSection();
   }

   public void clearCache() {
      this.cachedLevelChunks.clear();
      this.cachedChunkObj = null;
      this.cachedChunkPos = 0L;
   }

   public void setBlockFast(BlockPos blockPos, BlockState blockState) {
      LevelChunk chunk = this.getChunk(blockPos);
      BlockState blockState2 = chunk.setBlockState(blockPos, blockState, false);
      if (blockState2 != null) {
         this.level.sendBlockUpdated(blockPos, blockState2, blockState, 3);
      }
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos blockPos) {
      return this.level.getBlockEntity(blockPos);
   }

   public BlockState getBlockState(BlockPos pos) {
      LevelChunk chunk = this.getChunk(pos);
      return this.getBlockState(chunk, pos);
   }

   public BlockState getBlockState(LevelChunk chunk, BlockPos pos) {
      if (pos.getY() >= this.minBuildHeight && pos.getY() < this.maxBuildHeight) {
         LevelChunkSection section = chunk.getSection((pos.getY() >> 4) - this.minSection);
         return section.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
      } else {
         return Blocks.AIR.defaultBlockState();
      }
   }

   public FluidState getFluidState(BlockPos pos) {
      LevelChunk chunk = this.getChunk(pos);
      return chunk.getFluidState(pos);
   }

   public LevelChunk getChunk(BlockPos pos) {
      return this.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
   }

   public LevelChunk getChunk(int chunkX, int chunkZ) {
      long pos = ChunkPos.asLong(chunkX, chunkZ);
      if (pos == this.cachedChunkPos && this.cachedChunkObj != null) {
         return this.cachedChunkObj;
      } else {
         LevelChunk chunk = this.grabChunkFast(chunkX, chunkZ, pos);
         this.cachedChunkObj = chunk;
         this.cachedChunkPos = pos;
         return chunk;
      }
   }

   @NotNull
   private LevelChunk grabChunkFast(int chunkX, int chunkZ, long pos) {
      if (this.level.isClientSide) {
         return this.level.getChunk(chunkX, chunkZ);
      } else {
         ChunkHolder holder = ((ServerChunkCacheAccessor)this.level.getChunkSource()).invokeGetVisibleChunkIfPresent(pos);
         if (holder != null) {
            LevelChunk res = (LevelChunk)holder.getFullChunkFuture().getNow(ChunkResult.error("No chunk at position")).orElse(null);
            if (res != null) {
               return res;
            }
         }

         return this.level.getChunk(chunkX, chunkZ);
      }
   }

   public boolean isOutsideBuildHeight(Vec3i pos) {
      return pos.getY() < this.minBuildHeight || pos.getY() >= this.maxBuildHeight;
   }

   public int getHeight() {
      return this.level.getHeight();
   }

   public int getMinBuildHeight() {
      return this.minBuildHeight;
   }
}
