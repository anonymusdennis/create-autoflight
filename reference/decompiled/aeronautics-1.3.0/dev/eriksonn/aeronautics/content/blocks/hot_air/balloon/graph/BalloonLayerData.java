package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;

public class BalloonLayerData {
   public static final byte LAYER_CHUNK_SHIFT = 3;
   public static final byte LAYER_CHUNK_MASK = 7;
   private BalloonLayerData.State state = BalloonLayerData.State.NEEDS_DOWN_PASS;
   private final Long2LongMap chunks = new Long2LongOpenHashMap();
   private final Long2LongMap solidChunks = new Long2LongOpenHashMap();
   public final Collection<BalloonLayerData> inwardConnections = new ObjectArraySet();
   public final Collection<BalloonLayerData> outwardConnections = new ObjectArraySet();
   private final int yLevel;
   public int hotAirCount;
   public int solidCount;
   public boolean boundsInitialized = false;
   public int minChunkX;
   public int minChunkZ;
   public int maxChunkX;
   public int maxChunkZ;

   public static int blockToChunkCoord(int x) {
      return x >> 3;
   }

   public static int getBlockIndex(int x, int z) {
      return ((x & 7) << 3) + (z & 7);
   }

   public static long asLong(int x, int z) {
      return (long)x & 4294967295L | ((long)z & 4294967295L) << 32;
   }

   public static int getChunkX(long l) {
      return (int)(l & 4294967295L);
   }

   public static int getChunkZ(long l) {
      return (int)(l >>> 32 & 4294967295L);
   }

   public BalloonLayerData(int yLevel) {
      this.yLevel = yLevel;
   }

   public long getHotAirChunkAtBlock(int x, int z) {
      return this.chunks.get(asLong(x >> 3, z >> 3));
   }

   public long getSolidsChunkAtBlock(int x, int z) {
      return this.solidChunks.get(asLong(x >> 3, z >> 3));
   }

   public void addHotAirBlock(int x, int z) {
      int chunkX = x >> 3;
      int chunkZ = z >> 3;
      long key = asLong(chunkX, chunkZ);
      long l = this.chunks.get(key);
      if (l == 0L) {
         if (!this.boundsInitialized) {
            this.minChunkX = chunkX;
            this.minChunkZ = chunkZ;
            this.maxChunkX = chunkX;
            this.maxChunkZ = chunkZ;
            this.boundsInitialized = true;
         } else {
            this.minChunkX = Math.min(this.minChunkX, chunkX);
            this.minChunkZ = Math.min(this.minChunkZ, chunkZ);
            this.maxChunkX = Math.max(this.maxChunkX, chunkX);
            this.maxChunkZ = Math.max(this.maxChunkZ, chunkZ);
         }
      }

      this.chunks.put(key, l | 1L << getBlockIndex(x, z));
   }

   public void addSolidBlock(int x, int z) {
      long key = asLong(x >> 3, z >> 3);
      long l = this.solidChunks.get(key);
      this.solidChunks.put(key, l | 1L << getBlockIndex(x, z));
   }

   public void removeHotAirBlock(int x, int z) {
      long key = asLong(x >> 3, z >> 3);
      long l = this.chunks.get(key);
      if (l != 0L) {
         this.chunks.put(key, l & ~(1L << getBlockIndex(x, z)));
      }
   }

   public void removeSolidBlock(int x, int z) {
      long key = asLong(x >> 3, z >> 3);
      long l = this.solidChunks.get(key);
      if (l != 0L) {
         this.solidChunks.put(key, l & ~(1L << getBlockIndex(x, z)));
      }
   }

   public boolean overlaps(BalloonLayerData other) {
      if (this.boundsInitialized && other.boundsInitialized) {
         int minChunkX = Math.max(this.minChunkX, other.minChunkX);
         int minChunkZ = Math.max(this.minChunkZ, other.minChunkZ);
         int maxChunkX = Math.min(this.maxChunkX, other.maxChunkX);
         int maxChunkZ = Math.min(this.maxChunkZ, other.maxChunkZ);
         if (minChunkX <= maxChunkX && minChunkZ <= maxChunkZ) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
               for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                  long key = asLong(chunkX, chunkZ);
                  long thisBits = this.chunks.get(key);
                  long otherBits = other.chunks.get(key);
                  if ((thisBits & otherBits) != 0L) {
                     return true;
                  }
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public BalloonLayerData.State getState() {
      return this.state;
   }

   public void setState(BalloonLayerData.State state) {
      this.state = state;
   }

   public boolean getHotAirBlock(int x, int z) {
      long chunk = this.getHotAirChunkAtBlock(x, z);
      if (chunk == 0L) {
         return false;
      } else {
         int blockIndex = getBlockIndex(x, z);
         return (chunk >> blockIndex & 1L) != 0L;
      }
   }

   public boolean getSolidBlock(int x, int z) {
      long chunk = this.getSolidsChunkAtBlock(x, z);
      if (chunk == 0L) {
         return false;
      } else {
         int blockIndex = getBlockIndex(x, z);
         return (chunk >> blockIndex & 1L) != 0L;
      }
   }

   public Long2LongMap getChunks() {
      return this.chunks;
   }

   public Long2LongMap getSolidChunks() {
      return this.solidChunks;
   }

   public int getYLevel() {
      return this.yLevel;
   }

   public Iterator<BlockPos> blockIterator() {
      return new BalloonLayerData.LayerBlockIterator(false);
   }

   public Iterator<BlockPos> nonSolidBlockIterator() {
      return new BalloonLayerData.LayerBlockIterator(true);
   }

   private final class LayerBlockIterator implements Iterator<BlockPos> {
      private final Iterator<Entry> chunkIter;
      private final boolean ignoreSolids;
      private Entry currentEntry;
      private boolean hasSolidsChunk;
      private long solidsChunkBits;
      private int bitIndex = -1;
      private boolean hasNext = false;
      private final MutableBlockPos nextPos = new MutableBlockPos();
      private final MutableBlockPos resultPos = new MutableBlockPos();

      private LayerBlockIterator(final boolean ignoreSolids) {
         this.chunkIter = Long2LongMaps.fastIterator(BalloonLayerData.this.chunks);
         this.ignoreSolids = ignoreSolids;
         this.advance();
      }

      private void advance() {
         this.hasNext = false;

         while (true) {
            if (this.currentEntry == null && this.chunkIter.hasNext()) {
               this.currentEntry = this.chunkIter.next();
               if (this.ignoreSolids) {
                  this.solidsChunkBits = BalloonLayerData.this.solidChunks.get(this.currentEntry.getLongKey());
                  this.hasSolidsChunk = this.solidsChunkBits != 0L;
               }

               this.bitIndex = -1;
            } else if (this.currentEntry == null) {
               return;
            }

            long chunkBits = this.currentEntry.getLongValue();
            if (this.hasSolidsChunk) {
               chunkBits &= ~this.solidsChunkBits;
            }

            boolean chunkFull = chunkBits == -1L;

            while (++this.bitIndex < 64) {
               if (chunkFull || (chunkBits >>> this.bitIndex & 1L) != 0L) {
                  long key = this.currentEntry.getLongKey();
                  int chunkX = BalloonLayerData.getChunkX(key);
                  int chunkZ = BalloonLayerData.getChunkZ(key);
                  int localX = this.bitIndex >> 3;
                  int localZ = this.bitIndex & 7;
                  int worldX = (chunkX << 3) + localX;
                  int worldZ = (chunkZ << 3) + localZ;
                  this.nextPos.set(worldX, BalloonLayerData.this.yLevel, worldZ);
                  this.hasNext = true;
                  return;
               }
            }

            this.currentEntry = null;
         }
      }

      @Override
      public boolean hasNext() {
         return this.hasNext;
      }

      public BlockPos next() {
         if (!this.hasNext) {
            throw new NoSuchElementException();
         } else {
            this.resultPos.set(this.nextPos);
            this.advance();
            return this.resultPos;
         }
      }
   }

   public static enum State {
      NEEDS_DOWN_PASS,
      COMPLETE;
   }
}
