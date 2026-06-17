package dev.ryanhcode.sable.mixinhelpers.voxel_shape_iteration;

import java.util.Arrays;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class LongArrayDiscreteVoxelShape extends DiscreteVoxelShape {
   private static final int ADDRESS_BITS_PER_WORD = 6;
   private static final int BITS_PER_WORD = 64;
   private static final long WORD_MASK = -1L;
   private final long[] baseWords;
   private final long[] words;

   public LongArrayDiscreteVoxelShape(DiscreteVoxelShape shape, int xSize, int ySize, int zSize) {
      super(xSize, ySize, zSize);
      this.words = new long[wordIndex(xSize * ySize * zSize - 1) + 1];

      for (int x = 0; x < xSize; x++) {
         for (int y = 0; y < ySize; y++) {
            for (int z = 0; z < zSize; z++) {
               if (shape.isFull(x, y, z)) {
                  int bitIndex = this.getIndex(x, y, z);
                  this.words[wordIndex(bitIndex)] |= 1L << bitIndex;
               }
            }
         }
      }

      this.baseWords = Arrays.copyOf(this.words, this.words.length);
   }

   private static int wordIndex(int bitIndex) {
      return bitIndex >> 6;
   }

   private int getIndex(int x, int y, int z) {
      return (x * this.ySize + y) * this.zSize + z;
   }

   private int nextClearBit(int fromIndex) {
      int u = wordIndex(fromIndex);

      long word;
      for (word = ~this.words[u] & -1L << fromIndex; word == 0L; word = ~this.words[u]) {
         if (++u == this.words.length) {
            return this.words.length * 64;
         }
      }

      return u * 64 + Long.numberOfTrailingZeros(word);
   }

   private void clear(int fromIndex, int toIndex) {
      int startWordIndex = wordIndex(fromIndex);
      int endWordIndex = wordIndex(toIndex - 1);
      if (endWordIndex >= this.words.length) {
         toIndex = 64 * (this.words.length - 1) + (64 - Long.numberOfLeadingZeros(this.words[this.words.length - 1]));
         endWordIndex = this.words.length - 1;
      }

      long firstWordMask = -1L << fromIndex;
      long lastWordMask = -1L >>> -toIndex;
      if (startWordIndex == endWordIndex) {
         this.words[startWordIndex] = this.words[startWordIndex] & ~(firstWordMask & lastWordMask);
      } else {
         this.words[startWordIndex] = this.words[startWordIndex] & ~firstWordMask;

         for (int i = startWordIndex + 1; i < endWordIndex; i++) {
            this.words[i] = 0L;
         }

         this.words[endWordIndex] = this.words[endWordIndex] & ~lastWordMask;
      }
   }

   public void reset() {
      System.arraycopy(this.baseWords, 0, this.words, 0, this.words.length);
   }

   public boolean isZStripFull(int i, int j, int k, int l) {
      return k < this.xSize && l < this.ySize && this.nextClearBit(this.getIndex(k, l, i)) >= this.getIndex(k, l, j);
   }

   public boolean isXZRectangleFull(int i, int j, int k, int l, int m) {
      for (int n = i; n < j; n++) {
         if (!this.isZStripFull(k, l, n, m)) {
            return false;
         }
      }

      return true;
   }

   public void clearZStrip(int i, int j, int k, int l) {
      this.clear(this.getIndex(k, l, i), this.getIndex(k, l, j));
   }

   public boolean isFull(int x, int y, int z) {
      int bitIndex = this.getIndex(x, y, z);
      int wordIndex = wordIndex(bitIndex);
      return wordIndex < this.words.length && (this.words[wordIndex] & 1L << bitIndex) != 0L;
   }

   public void fill(int i, int j, int k) {
      throw new UnsupportedOperationException();
   }

   public int firstFull(Axis axis) {
      throw new UnsupportedOperationException();
   }

   public int lastFull(Axis axis) {
      throw new UnsupportedOperationException();
   }
}
