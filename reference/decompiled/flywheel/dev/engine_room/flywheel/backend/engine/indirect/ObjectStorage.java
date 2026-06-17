package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.backend.engine.AbstractArena;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import java.util.Arrays;
import java.util.BitSet;
import org.lwjgl.system.MemoryUtil;

public class ObjectStorage extends AbstractArena {
   public static final int LOG_2_PAGE_SIZE = 5;
   public static final int PAGE_SIZE = 32;
   public static final int PAGE_MASK = 31;
   public static final int INVALID_PAGE = -1;
   public static final int INITIAL_PAGES_ALLOCATED = 4;
   public static final int DESCRIPTOR_SIZE_BYTES = 8;
   private final BitSet changedFrames = new BitSet();
   public final ResizableStorageBuffer objectBuffer = new ResizableStorageBuffer();
   public final ResizableStorageBuffer frameDescriptorBuffer = new ResizableStorageBuffer();
   private MemoryBlock frameDescriptors;

   public ObjectStorage(long objectSizeBytes) {
      super(32L * objectSizeBytes);
      this.objectBuffer.ensureCapacity(4L * this.elementSizeBytes);
      this.frameDescriptorBuffer.ensureCapacity(32L);
      this.frameDescriptors = MemoryBlock.malloc(32L);
   }

   public ObjectStorage.Mapping createMapping() {
      return new ObjectStorage.Mapping();
   }

   @Override
   public long byteCapacity() {
      return this.objectBuffer.capacity();
   }

   @Override
   public void free(int i) {
      if (i != -1) {
         super.free(i);
         long ptr = this.ptrForPage(i);
         MemoryUtil.memPutInt(ptr, 0);
         MemoryUtil.memPutInt(ptr + 4L, 0);
         this.changedFrames.set(i);
      }
   }

   private void set(int i, int modelIndex, int validBits) {
      long ptr = this.ptrForPage(i);
      MemoryUtil.memPutInt(ptr, modelIndex);
      MemoryUtil.memPutInt(ptr + 4L, validBits);
      this.changedFrames.set(i);
   }

   @Override
   protected void grow() {
      this.objectBuffer.ensureCapacity(this.objectBuffer.capacity() * 2L);
      this.frameDescriptorBuffer.ensureCapacity(this.frameDescriptorBuffer.capacity() * 2L);
      this.frameDescriptors = this.frameDescriptors.realloc(this.frameDescriptors.size() * 2L);
   }

   public void uploadDescriptors(StagingBuffer stagingBuffer) {
      if (!this.changedFrames.isEmpty()) {
         long ptr = this.frameDescriptors.ptr();

         for (int i = this.changedFrames.nextSetBit(0); i >= 0 && i < this.capacity(); i = this.changedFrames.nextSetBit(i + 1)) {
            long offset = (long)i * 8L;
            stagingBuffer.enqueueCopy(ptr + offset, 8L, this.frameDescriptorBuffer.handle(), offset);
         }

         this.changedFrames.clear();
      }
   }

   public void delete() {
      this.objectBuffer.delete();
      this.frameDescriptorBuffer.delete();
      this.frameDescriptors.free();
   }

   private long ptrForPage(int page) {
      return this.frameDescriptors.ptr() + (long)page * 8L;
   }

   public static int objectIndex2PageIndex(int objectIndex) {
      return objectIndex >> 5;
   }

   public static int pageIndex2ObjectIndex(int pageIndex) {
      return pageIndex << 5;
   }

   public class Mapping {
      private static final int[] EMPTY_ALLOCATION = new int[0];
      private int[] pages = EMPTY_ALLOCATION;

      public void updatePage(int index, int modelIndex, int validBits) {
         if (validBits == 0) {
            this.holePunch(index);
         } else {
            int frame = this.pages[index];
            if (frame == -1) {
               frame = this.unHolePunch(index);
            }

            ObjectStorage.this.set(frame, modelIndex, validBits);
         }
      }

      public void holePunch(int index) {
         ObjectStorage.this.free(this.pages[index]);
         this.pages[index] = -1;
      }

      private int unHolePunch(int index) {
         int page = ObjectStorage.this.alloc();
         this.pages[index] = page;
         return page;
      }

      public void updateCount(int newLength) {
         int oldLength = this.pages.length;
         if (oldLength > newLength) {
            this.shrink(oldLength, newLength);
         } else if (oldLength < newLength) {
            this.grow(newLength, oldLength);
         }
      }

      public int pageCount() {
         return this.pages.length;
      }

      public long page2ByteOffset(int index) {
         return ObjectStorage.this.byteOffsetOf(this.pages[index]);
      }

      public void delete() {
         for (int page : this.pages) {
            ObjectStorage.this.free(page);
         }

         this.pages = EMPTY_ALLOCATION;
      }

      private void grow(int neededPages, int oldLength) {
         this.pages = Arrays.copyOf(this.pages, neededPages);

         for (int i = oldLength; i < neededPages; i++) {
            int page = ObjectStorage.this.alloc();
            this.pages[i] = page;
         }
      }

      private void shrink(int oldLength, int neededPages) {
         for (int i = oldLength - 1; i >= neededPages; i--) {
            int page = this.pages[i];
            ObjectStorage.this.free(page);
         }

         this.pages = Arrays.copyOf(this.pages, neededPages);
      }

      public int objectIndex2GlobalIndex(int objectIndex) {
         return (this.pages[ObjectStorage.objectIndex2PageIndex(objectIndex)] << 5) + (objectIndex & 31);
      }
   }
}
