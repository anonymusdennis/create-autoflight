package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import org.lwjgl.opengl.GL44;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

public class IndirectBuffers {
   public static final int BUFFER_COUNT = 5;
   public static final long INT_SIZE = 4L;
   public static final long PTR_SIZE = (long)Pointer.POINTER_SIZE;
   public static final long MODEL_STRIDE = 28L;
   public static final long DRAW_COMMAND_STRIDE = 36L;
   public static final long DRAW_COMMAND_OFFSET = 0L;
   private static final long HANDLE_OFFSET = 0L;
   private static final long OFFSET_OFFSET = 20L;
   private static final long SIZE_OFFSET = 20L + 5L * PTR_SIZE;
   private static final long BUFFERS_SIZE_BYTES = SIZE_OFFSET + 5L * PTR_SIZE;
   private static final long PAGE_FRAME_DESCRIPTOR_HANDLE_OFFSET = 0L;
   private static final long INSTANCE_HANDLE_OFFSET = 4L;
   private static final long DRAW_INSTANCE_INDEX_HANDLE_OFFSET = 8L;
   private static final long MODEL_HANDLE_OFFSET = 12L;
   private static final long DRAW_HANDLE_OFFSET = 16L;
   private static final long PAGE_FRAME_DESCRIPTOR_SIZE_OFFSET = SIZE_OFFSET + 0L * PTR_SIZE;
   private static final long INSTANCE_SIZE_OFFSET = SIZE_OFFSET + 1L * PTR_SIZE;
   private static final long DRAW_INSTANCE_INDEX_SIZE_OFFSET = SIZE_OFFSET + 2L * PTR_SIZE;
   private static final long MODEL_SIZE_OFFSET = SIZE_OFFSET + 3L * PTR_SIZE;
   private static final long DRAW_SIZE_OFFSET = SIZE_OFFSET + 4L * PTR_SIZE;
   private static final float INSTANCE_GROWTH_FACTOR = 1.25F;
   private static final float MODEL_GROWTH_FACTOR = 2.0F;
   private static final float DRAW_GROWTH_FACTOR = 2.0F;
   private final MemoryBlock multiBindBlock = MemoryBlock.calloc(BUFFERS_SIZE_BYTES, 1L);
   public final ObjectStorage objectStorage;
   public final ResizableStorageArray drawInstanceIndex;
   public final ResizableStorageArray model;
   public final ResizableStorageArray draw;

   IndirectBuffers(long instanceStride) {
      this.objectStorage = new ObjectStorage(instanceStride);
      this.drawInstanceIndex = new ResizableStorageArray(4L, 1.25);
      this.model = new ResizableStorageArray(28L, 2.0);
      this.draw = new ResizableStorageArray(36L, 2.0);
   }

   void updateCounts(int instanceCount, int modelCount, int drawCount) {
      this.drawInstanceIndex.ensureCapacity((long)instanceCount);
      this.model.ensureCapacity((long)modelCount);
      this.draw.ensureCapacity((long)drawCount);
      long ptr = this.multiBindBlock.ptr();
      MemoryUtil.memPutInt(ptr + 0L, this.objectStorage.frameDescriptorBuffer.handle());
      MemoryUtil.memPutInt(ptr + 4L, this.objectStorage.objectBuffer.handle());
      MemoryUtil.memPutInt(ptr + 8L, this.drawInstanceIndex.handle());
      MemoryUtil.memPutInt(ptr + 12L, this.model.handle());
      MemoryUtil.memPutInt(ptr + 16L, this.draw.handle());
      MemoryUtil.memPutAddress(ptr + PAGE_FRAME_DESCRIPTOR_SIZE_OFFSET, this.objectStorage.frameDescriptorBuffer.capacity());
      MemoryUtil.memPutAddress(ptr + INSTANCE_SIZE_OFFSET, this.objectStorage.objectBuffer.capacity());
      MemoryUtil.memPutAddress(ptr + DRAW_INSTANCE_INDEX_SIZE_OFFSET, 4L * (long)instanceCount);
      MemoryUtil.memPutAddress(ptr + MODEL_SIZE_OFFSET, 28L * (long)modelCount);
      MemoryUtil.memPutAddress(ptr + DRAW_SIZE_OFFSET, 36L * (long)drawCount);
   }

   public void bindForCull() {
      this.multiBind(0, 4);
   }

   public void bindForApply() {
      this.multiBind(3, 2);
   }

   public void bindForDraw() {
      this.multiBind(1, 4);
      GlBufferType.DRAW_INDIRECT_BUFFER.bind(this.draw.handle());
   }

   public void bindForCrumbling() {
      this.multiBind(1, 1);
   }

   private void multiBind(int base, int count) {
      long ptr = this.multiBindBlock.ptr();
      GL44.nglBindBuffersRange(37074, base, count, ptr + (long)base * 4L, ptr + 20L + (long)base * PTR_SIZE, ptr + SIZE_OFFSET + (long)base * PTR_SIZE);
   }

   public void delete() {
      this.multiBindBlock.free();
      this.objectStorage.delete();
      this.drawInstanceIndex.delete();
      this.model.delete();
      this.draw.delete();
   }
}
