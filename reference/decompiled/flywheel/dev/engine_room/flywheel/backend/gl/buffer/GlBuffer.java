package dev.engine_room.flywheel.backend.gl.buffer;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.engine_room.flywheel.backend.gl.GlObject;
import dev.engine_room.flywheel.lib.memory.FlwMemoryTracker;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;

public class GlBuffer extends GlObject {
   protected final GlBufferUsage usage;
   protected long size;

   public GlBuffer(GlBufferUsage usage) {
      this.handle(Buffer.IMPL.create());
      this.usage = usage;
   }

   public void upload(MemoryBlock memoryBlock) {
      this.upload(memoryBlock.ptr(), memoryBlock.size());
   }

   public void upload(long ptr, long size) {
      FlwMemoryTracker._freeGpuMemory(this.size);
      Buffer.IMPL.data(this.handle(), size, ptr, this.usage.glEnum);
      this.size = size;
      FlwMemoryTracker._allocGpuMemory(this.size);
   }

   public void uploadSpan(long offset, MemoryBlock memoryBlock) {
      this.uploadSpan(offset, memoryBlock.ptr(), memoryBlock.size());
   }

   public void uploadSpan(long offset, long ptr, long size) {
      Buffer.IMPL.subData(this.handle(), offset, size, ptr);
   }

   public long size() {
      return this.size;
   }

   @Override
   protected void deleteInternal(int handle) {
      GlStateManager._glDeleteBuffers(handle);
      FlwMemoryTracker._freeGpuMemory(this.size);
   }
}
