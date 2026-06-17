package dev.engine_room.flywheel.backend.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

public class AtomicBitSet {
   public static final int DEFAULT_LOG2_SEGMENT_SIZE_IN_BITS = 10;
   private static final long WORD_MASK = -1L;
   private static final VarHandle AA = MethodHandles.arrayElementVarHandle(long[].class);
   private final int numLongsPerSegment;
   private final int log2SegmentSize;
   private final int segmentMask;
   private final AtomicReference<AtomicBitSet.AtomicBitSetSegments> segments;

   public AtomicBitSet() {
      this(10);
   }

   public AtomicBitSet(int log2SegmentSizeInBits) {
      this(log2SegmentSizeInBits, 0);
   }

   public AtomicBitSet(int log2SegmentSizeInBits, int numBitsToPreallocate) {
      if (log2SegmentSizeInBits < 6) {
         throw new IllegalArgumentException("Cannot specify fewer than 64 bits in each segment!");
      } else {
         this.log2SegmentSize = log2SegmentSizeInBits;
         this.numLongsPerSegment = 1 << log2SegmentSizeInBits - 6;
         this.segmentMask = this.numLongsPerSegment - 1;
         long numBitsPerSegment = (long)this.numLongsPerSegment * 64L;
         int numSegmentsToPreallocate = numBitsToPreallocate == 0 ? 1 : (int)((long)(numBitsToPreallocate - 1) / numBitsPerSegment + 1L);
         this.segments = new AtomicReference<>(new AtomicBitSet.AtomicBitSetSegments(numSegmentsToPreallocate, this.numLongsPerSegment));
      }
   }

   public void set(int position, boolean value) {
      if (value) {
         this.set(position);
      } else {
         this.clear(position);
      }
   }

   public void set(int position) {
      if (position >= 0) {
         int longPosition = this.longIndexInSegmentForPosition(position);
         long[] segment = this.getOrCreateSegmentForPosition(position);
         this.setOr(segment, longPosition, maskForPosition(position));
      }
   }

   public void clear(int position) {
      if (position >= 0) {
         int longPosition = this.longIndexInSegmentForPosition(position);
         int segmentIndex = this.segmentIndexForPosition(position);
         AtomicBitSet.AtomicBitSetSegments segments = this.segments.get();
         if (segmentIndex < segments.numSegments()) {
            this.setAnd(segments.getSegment(segmentIndex), longPosition, ~maskForPosition(position));
         }
      }
   }

   public void set(int fromIndex, int toIndex) {
      if (toIndex > fromIndex) {
         int firstSegmentIndex = this.segmentIndexForPosition(fromIndex);
         int toSegmentIndex = this.segmentIndexForPosition(toIndex - 1);
         AtomicBitSet.AtomicBitSetSegments segments = this.expandToFit(toSegmentIndex);
         int fromLongIndex = this.longIndexInSegmentForPosition(fromIndex);
         int toLongIndex = this.longIndexInSegmentForPosition(toIndex - 1);
         long fromLongMask = -1L << fromIndex;
         long toLongMask = -1L >>> -toIndex;
         long[] segment = segments.getSegment(firstSegmentIndex);
         if (firstSegmentIndex == toSegmentIndex) {
            if (fromLongIndex == toLongIndex) {
               this.setOr(segment, fromLongIndex, fromLongMask & toLongMask);
            } else {
               this.setOr(segment, fromLongIndex, fromLongMask);

               for (int i = fromLongIndex + 1; i < toLongIndex; i++) {
                  AA.setRelease((long[])segment, (int)i, (long)-1L);
               }

               this.setOr(segment, toLongIndex, toLongMask);
            }
         } else {
            this.setOr(segment, fromLongIndex, fromLongMask);

            for (int i = fromLongIndex + 1; i < this.numLongsPerSegment; i++) {
               AA.setRelease((long[])segment, (int)i, (long)-1L);
            }

            for (int i = firstSegmentIndex + 1; i < toSegmentIndex; i++) {
               segment = segments.getSegment(i);

               for (int j = 0; j < segment.length; j++) {
                  AA.setRelease((long[])segment, (int)j, (long)-1L);
               }
            }

            segment = segments.getSegment(toSegmentIndex);

            for (int i = 0; i < toLongIndex; i++) {
               AA.setRelease((long[])segment, (int)i, (long)-1L);
            }

            this.setOr(segment, toLongIndex, toLongMask);
         }
      }
   }

   public void clear(int fromIndex, int toIndex) {
      if (toIndex > fromIndex) {
         AtomicBitSet.AtomicBitSetSegments segments = this.segments.get();
         int numSegments = segments.numSegments();
         int firstSegmentIndex = this.segmentIndexForPosition(fromIndex);
         if (firstSegmentIndex < numSegments) {
            int toSegmentIndex = this.segmentIndexForPosition(toIndex - 1);
            if (toSegmentIndex >= numSegments) {
               toSegmentIndex = numSegments - 1;
               toIndex = numSegments * (1 << this.log2SegmentSize);
            }

            int fromLongIndex = this.longIndexInSegmentForPosition(fromIndex);
            int toLongIndex = this.longIndexInSegmentForPosition(toIndex - 1);
            long fromLongMask = -1L << fromIndex;
            long toLongMask = -1L >>> -toIndex;
            long[] segment = segments.getSegment(firstSegmentIndex);
            if (firstSegmentIndex == toSegmentIndex) {
               if (fromLongIndex == toLongIndex) {
                  this.setAnd(segment, fromLongIndex, ~(fromLongMask & toLongMask));
               } else {
                  this.setAnd(segment, fromLongIndex, ~fromLongMask);

                  for (int i = fromLongIndex + 1; i < toLongIndex; i++) {
                     AA.setRelease((long[])segment, (int)i, (int)0);
                  }

                  this.setAnd(segment, toLongIndex, ~toLongMask);
               }
            } else {
               this.setAnd(segment, fromLongIndex, ~fromLongMask);

               for (int i = fromLongIndex + 1; i < this.numLongsPerSegment; i++) {
                  AA.setRelease((long[])segment, (int)i, (int)0);
               }

               for (int i = firstSegmentIndex + 1; i < toSegmentIndex; i++) {
                  segment = segments.getSegment(i);

                  for (int j = 0; j < segment.length; j++) {
                     AA.setRelease((long[])segment, (int)j, (int)0);
                  }
               }

               segment = segments.getSegment(toSegmentIndex);

               for (int i = 0; i < toLongIndex; i++) {
                  AA.setRelease((long[])segment, (int)i, (int)0);
               }

               this.setAnd(segment, toLongIndex, ~toLongMask);
            }
         }
      }
   }

   private void setOr(long[] segment, int indexInSegment, long mask) {
      AA.getAndBitwiseOrRelease((long[])segment, (int)indexInSegment, (long)mask);
   }

   private void setAnd(long[] segment, int indexInSegment, long mask) {
      AA.getAndBitwiseAndRelease((long[])segment, (int)indexInSegment, (long)mask);
   }

   public boolean get(int position) {
      int segmentPosition = this.segmentIndexForPosition(position);
      int longPosition = this.longIndexInSegmentForPosition(position);
      long[] segment = this.segmentForPosition(segmentPosition);
      long mask = maskForPosition(position);
      return ((long)AA.getAcquire((long[])segment, (int)longPosition) & mask) != 0L;
   }

   public long maxSetBit() {
      AtomicBitSet.AtomicBitSetSegments segments = this.segments.get();

      for (int segmentIdx = segments.numSegments() - 1; segmentIdx >= 0; segmentIdx--) {
         long[] segment = segments.getSegment(segmentIdx);

         for (int longIdx = segment.length - 1; longIdx >= 0; longIdx--) {
            long l = (long)AA.getAcquire((long[])segment, (int)longIdx);
            if (l != 0L) {
               return ((long)segmentIdx << this.log2SegmentSize) + (long)longIdx * 64L + (long)(63 - Long.numberOfLeadingZeros(l));
            }
         }
      }

      return -1L;
   }

   public int nextSetBit(int fromIndex) {
      if (fromIndex < 0) {
         throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
      } else {
         AtomicBitSet.AtomicBitSetSegments segments = this.segments.get();
         int segmentPosition = this.segmentIndexForPosition(fromIndex);
         if (segmentPosition >= segments.numSegments()) {
            return -1;
         } else {
            int longPosition = this.longIndexInSegmentForPosition(fromIndex);
            long[] segment = segments.getSegment(segmentPosition);

            long word;
            for (word = (long)AA.getAcquire((long[])segment, (int)longPosition) & -1L << bitPosInLongForPosition(fromIndex);
               word == 0L;
               word = (long)AA.getAcquire((long[])segment, (int)longPosition)
            ) {
               if (++longPosition > this.segmentMask) {
                  if (++segmentPosition >= segments.numSegments()) {
                     return -1;
                  }

                  segment = segments.getSegment(segmentPosition);
                  longPosition = 0;
               }
            }

            return (segmentPosition << this.log2SegmentSize) + (longPosition << 6) + Long.numberOfTrailingZeros(word);
         }
      }
   }

   public int nextClearBit(int fromIndex) {
      if (fromIndex < 0) {
         throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
      } else {
         int segmentPosition = this.segmentIndexForPosition(fromIndex);
         AtomicBitSet.AtomicBitSetSegments segments = this.segments.get();
         if (segmentPosition >= segments.numSegments()) {
            return fromIndex;
         } else {
            int longPosition = this.longIndexInSegmentForPosition(fromIndex);
            long[] segment = segments.getSegment(segmentPosition);

            long word;
            for (word = ~(long)AA.getAcquire((long[])segment, (int)longPosition) & -1L << bitPosInLongForPosition(fromIndex);
               word == 0L;
               word = ~(long)AA.getAcquire((long[])segment, (int)longPosition)
            ) {
               if (++longPosition > this.segmentMask) {
                  if (++segmentPosition >= segments.numSegments()) {
                     return segments.numSegments() << this.log2SegmentSize + (longPosition << 6);
                  }

                  segment = segments.getSegment(segmentPosition);
                  longPosition = 0;
               }
            }

            return (segmentPosition << this.log2SegmentSize) + (longPosition << 6) + Long.numberOfTrailingZeros(word);
         }
      }
   }

   public int cardinality() {
      return this.segments.get().cardinality();
   }

   public void forEachSetSpan(AtomicBitSet.BitSpanConsumer consumer) {
      AtomicBitSet.AtomicBitSetSegments segments = this.segments.get();
      int start = -1;
      int end = -1;

      for (int segmentIndex = 0; segmentIndex < segments.numSegments(); segmentIndex++) {
         long[] segment = segments.getSegment(segmentIndex);

         for (int longIndex = 0; longIndex < segment.length; longIndex++) {
            long l = (long)AA.getAcquire((long[])segment, (int)longIndex);
            if (l != 0L) {
               for (int bitIndex = 0; bitIndex < 64; bitIndex++) {
                  if ((l & 1L << bitIndex) != 0L) {
                     int position = (segmentIndex << this.log2SegmentSize) + (longIndex << 6) + bitIndex;
                     if (start == -1) {
                        start = position;
                     }

                     end = position;
                  } else if (start != -1) {
                     consumer.accept(start, end);
                     start = -1;
                     end = -1;
                  }
               }
            } else if (start != -1) {
               consumer.accept(start, end);
               start = -1;
               end = -1;
            }
         }
      }

      if (start != -1) {
         consumer.accept(start, end);
      }
   }

   public int currentCapacity() {
      return this.segments.get().numSegments() * (1 << this.log2SegmentSize);
   }

   public boolean isEmpty() {
      return this.segments.get().isEmpty();
   }

   public void clear() {
      AtomicBitSet.AtomicBitSetSegments segments = this.segments.get();

      for (int i = 0; i < segments.numSegments(); i++) {
         long[] segment = segments.getSegment(i);

         for (int j = 0; j < segment.length; j++) {
            AA.setRelease((long[])segment, (int)j, (long)0L);
         }
      }
   }

   private static int bitPosInLongForPosition(int position) {
      return position & 63;
   }

   private int longIndexInSegmentForPosition(int position) {
      return position >>> 6 & this.segmentMask;
   }

   private int segmentIndexForPosition(int position) {
      return position >>> this.log2SegmentSize;
   }

   private static long maskForPosition(int position) {
      return 1L << bitPosInLongForPosition(position);
   }

   private long[] getOrCreateSegmentForPosition(int position) {
      return this.segmentForPosition(this.segmentIndexForPosition(position));
   }

   private long[] segmentForPosition(int segmentIndex) {
      return this.expandToFit(segmentIndex).getSegment(segmentIndex);
   }

   @NotNull
   private AtomicBitSet.AtomicBitSetSegments expandToFit(int segmentIndex) {
      AtomicBitSet.AtomicBitSetSegments visibleSegments = this.segments.get();

      while (visibleSegments.numSegments() <= segmentIndex) {
         AtomicBitSet.AtomicBitSetSegments newVisibleSegments = new AtomicBitSet.AtomicBitSetSegments(
            visibleSegments, segmentIndex + 1, this.numLongsPerSegment
         );
         if (this.segments.compareAndSet(visibleSegments, newVisibleSegments)) {
            visibleSegments = newVisibleSegments;
         } else {
            visibleSegments = this.segments.get();
         }
      }

      return visibleSegments;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof AtomicBitSet other)) {
         return false;
      } else if (other.log2SegmentSize != this.log2SegmentSize) {
         throw new IllegalArgumentException("Segment sizes must be the same");
      } else {
         AtomicBitSet.AtomicBitSetSegments thisSegments = this.segments.get();
         AtomicBitSet.AtomicBitSetSegments otherSegments = other.segments.get();

         for (int i = 0; i < thisSegments.numSegments(); i++) {
            long[] thisArray = thisSegments.getSegment(i);
            long[] otherArray = i < otherSegments.numSegments() ? otherSegments.getSegment(i) : null;

            for (int j = 0; j < thisArray.length; j++) {
               long thisLong = (long)AA.getAcquire((long[])thisArray, (int)j);
               long otherLong = otherArray == null ? 0L : (long)AA.getAcquire((long[])otherArray, (int)j);
               if (thisLong != otherLong) {
                  return false;
               }
            }
         }

         for (int i = thisSegments.numSegments(); i < otherSegments.numSegments(); i++) {
            long[] otherArray = otherSegments.getSegment(i);

            for (int jx = 0; jx < otherArray.length; jx++) {
               long l = (long)AA.getAcquire((long[])otherArray, (int)jx);
               if (l != 0L) {
                  return false;
               }
            }
         }

         return true;
      }
   }

   @Override
   public int hashCode() {
      int result = this.log2SegmentSize;
      return 31 * result + Arrays.deepHashCode(this.segments.get().segments);
   }

   public BitSet toBitSet() {
      BitSet resultSet = new BitSet();

      for (int ordinal = this.nextSetBit(0); ordinal != -1; ordinal = this.nextSetBit(ordinal + 1)) {
         resultSet.set(ordinal);
      }

      return resultSet;
   }

   @Override
   public String toString() {
      return this.toBitSet().toString();
   }

   private static class AtomicBitSetSegments {
      private final long[][] segments;

      private AtomicBitSetSegments(int numSegments, int segmentLength) {
         long[][] segments = new long[numSegments][];

         for (int i = 0; i < numSegments; i++) {
            segments[i] = new long[segmentLength];
         }

         this.segments = segments;
      }

      private AtomicBitSetSegments(AtomicBitSet.AtomicBitSetSegments copyFrom, int numSegments, int segmentLength) {
         long[][] segments = new long[numSegments][];

         for (int i = 0; i < numSegments; i++) {
            segments[i] = i < copyFrom.numSegments() ? copyFrom.getSegment(i) : new long[segmentLength];
         }

         this.segments = segments;
      }

      private int cardinality() {
         int numSetBits = 0;

         for (int i = 0; i < this.numSegments(); i++) {
            long[] segment = this.getSegment(i);

            for (int j = 0; j < segment.length; j++) {
               numSetBits += Long.bitCount((long)AtomicBitSet.AA.getAcquire((long[])segment, (int)j));
            }
         }

         return numSetBits;
      }

      private boolean isEmpty() {
         for (int i = 0; i < this.numSegments(); i++) {
            long[] segment = this.getSegment(i);

            for (int j = 0; j < segment.length; j++) {
               if ((long)AtomicBitSet.AA.getAcquire((long[])segment, (int)j) != 0L) {
                  return false;
               }
            }
         }

         return true;
      }

      public int numSegments() {
         return this.segments.length;
      }

      public long[] getSegment(int index) {
         return this.segments[index];
      }
   }

   @FunctionalInterface
   public interface BitSpanConsumer {
      void accept(int var1, int var2);
   }
}
