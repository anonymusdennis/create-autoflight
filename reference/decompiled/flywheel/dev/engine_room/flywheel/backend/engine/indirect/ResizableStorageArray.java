package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.lib.math.MoreMath;

public class ResizableStorageArray {
   private static final double DEFAULT_GROWTH_FACTOR = 1.25;
   private final ResizableStorageBuffer buffer;
   private final long stride;
   private final double growthFactor;
   private long capacity;

   public ResizableStorageArray(long stride) {
      this(stride, 1.25);
   }

   public ResizableStorageArray(long stride, double growthFactor) {
      this.stride = stride;
      this.growthFactor = growthFactor;
      if (stride <= 0L) {
         throw new IllegalArgumentException("Stride must be positive!");
      } else if (growthFactor <= 1.0) {
         throw new IllegalArgumentException("Growth factor must be greater than 1!");
      } else {
         this.buffer = new ResizableStorageBuffer();
      }
   }

   public int handle() {
      return this.buffer.handle();
   }

   public long stride() {
      return this.stride;
   }

   public long capacity() {
      return this.capacity;
   }

   public long byteCapacity() {
      return this.buffer.capacity();
   }

   public void ensureCapacity(long capacity) {
      if (capacity > this.capacity) {
         long newCapacity = this.grow(capacity);
         this.buffer.ensureCapacity(this.stride * newCapacity);
         this.capacity = newCapacity;
      }
   }

   public void delete() {
      this.buffer.delete();
   }

   private long grow(long capacity) {
      return MoreMath.ceilLong((double)capacity * this.growthFactor);
   }
}
