package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.gl.GlFence;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.lib.memory.FlwMemoryTracker;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayFIFOQueue;
import java.util.function.LongConsumer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryUtil;

public class StagingBuffer {
   private static final long DEFAULT_CAPACITY = 16777216L;
   private static final int STORAGE_FLAGS = 578;
   private static final int MAP_FLAGS = 90;
   private static final int SSBO_ALIGNMENT = GL45.glGetInteger(37087);
   private final int vbo;
   private final long map;
   private final long capacity;
   private final IndirectPrograms programs;
   private final StagingBuffer.OverflowStagingBuffer overflow = new StagingBuffer.OverflowStagingBuffer();
   private final TransferList transfers = new TransferList();
   private final PriorityQueue<StagingBuffer.FencedRegion> fencedRegions = new ObjectArrayFIFOQueue();
   private final GlBuffer scatterBuffer = new GlBuffer(GlBufferUsage.STREAM_COPY);
   private final ScatterList scatterList = new ScatterList();
   private long start = 0L;
   private long pos = 0L;
   private long usedCapacity = 0L;
   private long totalAvailable;
   @Nullable
   private MemoryBlock scratch;

   public StagingBuffer(IndirectPrograms programs) {
      this(16777216L, programs);
   }

   public StagingBuffer(long capacity, IndirectPrograms programs) {
      this.capacity = capacity;
      this.programs = programs;
      this.vbo = GL45C.glCreateBuffers();
      GL45C.glNamedBufferStorage(this.vbo, capacity, 578);
      this.map = GL45C.nglMapNamedBufferRange(this.vbo, 0L, capacity, 90);
      this.totalAvailable = capacity;
      FlwMemoryTracker._allocCpuMemory(capacity);
   }

   public void enqueueCopy(long size, int dstVbo, long dstOffset, LongConsumer write) {
      long direct = this.reserveForCopy(size, dstVbo, dstOffset);
      if (direct != 0L) {
         write.accept(direct);
      } else {
         MemoryBlock block = this.getScratch(size);
         write.accept(block.ptr());
         this.enqueueCopy(block.ptr(), size, dstVbo, dstOffset);
      }
   }

   public void enqueueCopy(long ptr, long size, int dstVbo, long dstOffset) {
      this.assertMultipleOf4(size);
      if (size > this.totalAvailable) {
         this.overflow.upload(ptr, size, dstVbo, dstOffset);
      } else {
         long remaining = this.capacity - this.pos;
         if (size > remaining) {
            long split = size - remaining;
            MemoryUtil.memCopy(ptr, this.map + this.pos, remaining);
            this.pushTransfer(dstVbo, this.pos, dstOffset, remaining);
            MemoryUtil.memCopy(ptr + remaining, this.map, split);
            this.pushTransfer(dstVbo, 0L, dstOffset + remaining, split);
            this.pos = split;
         } else {
            MemoryUtil.memCopy(ptr, this.map + this.pos, size);
            this.pushTransfer(dstVbo, this.pos, dstOffset, size);
            this.pos += size;
         }
      }
   }

   public long reserveForCopy(long size, int dstVbo, long dstOffset) {
      this.assertMultipleOf4(size);
      long remaining = this.capacity - this.pos;
      if (size <= remaining && size <= this.totalAvailable) {
         long out = this.map + this.pos;
         this.pushTransfer(dstVbo, this.pos, dstOffset, size);
         this.pos += size;
         return out;
      } else {
         return 0L;
      }
   }

   public void flush() {
      if (!this.transfers.isEmpty()) {
         this.flushUsedRegion();
         this.dispatchComputeCopies();
         this.transfers.reset();
         this.fencedRegions.enqueue(new StagingBuffer.FencedRegion(new GlFence(), this.usedCapacity));
         this.usedCapacity = 0L;
         this.start = this.pos;
      }
   }

   public void reclaim() {
      while (!this.fencedRegions.isEmpty()) {
         StagingBuffer.FencedRegion region = (StagingBuffer.FencedRegion)this.fencedRegions.first();
         if (region.fence.isSignaled()) {
            this.fencedRegions.dequeue();
            region.fence.delete();
            this.totalAvailable = this.totalAvailable + region.capacity;
            continue;
         }
         break;
      }
   }

   public void delete() {
      GL45C.glUnmapNamedBuffer(this.vbo);
      GL45C.glDeleteBuffers(this.vbo);
      this.overflow.delete();
      this.scatterBuffer.delete();
      if (this.scratch != null) {
         this.scratch.free();
      }

      this.transfers.delete();
      this.scatterList.delete();
      FlwMemoryTracker._freeCpuMemory(this.capacity);
   }

   public MemoryBlock getScratch(long size) {
      if (this.scratch == null) {
         this.scratch = MemoryBlock.malloc(size);
      } else if (this.scratch.size() < size) {
         this.scratch = this.scratch.realloc(size);
      }

      return this.scratch;
   }

   private void pushTransfer(int dstVbo, long srcOffset, long dstOffset, long size) {
      if (this.totalAvailable < size) {
         throw new IllegalStateException("Not enough available space to transfer");
      } else {
         this.transfers.push(dstVbo, srcOffset, dstOffset, size);
         this.usedCapacity += size;
         this.totalAvailable -= size;
      }
   }

   private void dispatchComputeCopies() {
      this.programs.getScatterProgram().bind();
      GL45.glBindBufferBase(37074, 1, this.vbo);
      int transferCount = this.transfers.length();

      for (int i = 0; i < transferCount; i++) {
         int dstVbo = this.transfers.vbo(i);
         this.scatterList.pushTransfer(this.transfers, i);
         int nextVbo = i == transferCount - 1 ? -1 : this.transfers.vbo(i + 1);
         if (dstVbo != nextVbo) {
            this.dispatchScatter(dstVbo);
         }
      }
   }

   private void dispatchScatter(int dstVbo) {
      long scatterSize = this.scatterList.usedBytes();
      long alignedPos = this.pos + (long)SSBO_ALIGNMENT - 1L - (this.pos + (long)SSBO_ALIGNMENT - 1L) % (long)SSBO_ALIGNMENT;
      long remaining = this.capacity - alignedPos;
      if (scatterSize <= remaining && scatterSize <= this.totalAvailable) {
         MemoryUtil.memCopy(this.scatterList.ptr(), this.map + alignedPos, scatterSize);
         GL45.glBindBufferRange(37074, 0, this.vbo, alignedPos, scatterSize);
         long alignmentCost = alignedPos - this.pos;
         this.usedCapacity += scatterSize + alignmentCost;
         this.totalAvailable -= scatterSize + alignmentCost;
         this.pos += scatterSize + alignmentCost;
      } else {
         this.scatterBuffer.upload(this.scatterList.ptr(), scatterSize);
         GL45.glBindBufferBase(37074, 0, this.scatterBuffer.handle());
      }

      GL45.glBindBufferBase(37074, 2, dstVbo);
      GL45.glDispatchCompute(this.scatterList.copyCount(), 1, 1);
      this.scatterList.reset();
   }

   private void assertMultipleOf4(long size) {
      if (size % 4L != 0L) {
         throw new IllegalArgumentException("Size must be a multiple of 4");
      }
   }

   private long sendCopyCommands() {
      long usedCapacity = 0L;

      for (int i = 0; i < this.transfers.length(); i++) {
         long size = this.transfers.size(i);
         usedCapacity += size;
         GL45C.glCopyNamedBufferSubData(this.vbo, this.transfers.vbo(i), this.transfers.srcOffset(i), this.transfers.dstOffset(i), size);
      }

      return usedCapacity;
   }

   private void flushUsedRegion() {
      if (this.pos < this.start) {
         GL45C.glFlushMappedNamedBufferRange(this.vbo, this.start, this.capacity - this.start);
         GL45C.glFlushMappedNamedBufferRange(this.vbo, 0L, this.pos);
      } else {
         GL45C.glFlushMappedNamedBufferRange(this.vbo, this.start, this.pos - this.start);
      }
   }

   private static record FencedRegion(GlFence fence, long capacity) {
   }

   private static class OverflowStagingBuffer {
      private final int vbo = GL45C.glCreateBuffers();

      public OverflowStagingBuffer() {
      }

      public void upload(long ptr, long size, int dstVbo, long dstOffset) {
         GL45C.nglNamedBufferData(this.vbo, size, ptr, 35042);
         GL45C.glCopyNamedBufferSubData(this.vbo, dstVbo, 0L, dstOffset, size);
      }

      public void delete() {
         GL45C.glDeleteBuffers(this.vbo);
      }
   }
}
