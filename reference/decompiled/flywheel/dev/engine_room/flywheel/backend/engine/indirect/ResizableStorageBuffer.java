package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.backend.gl.GlObject;
import dev.engine_room.flywheel.lib.memory.FlwMemoryTracker;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL45;

public class ResizableStorageBuffer extends GlObject {
   private long capacity = 0L;

   public ResizableStorageBuffer() {
      this.handle(GL45.glCreateBuffers());
   }

   public long capacity() {
      return this.capacity;
   }

   public void ensureCapacity(long capacity) {
      FlwMemoryTracker._freeGpuMemory(this.capacity);
      if (this.capacity > 0L) {
         int oldHandle = this.handle();
         int newHandle = GL45.glCreateBuffers();
         GL45.glNamedBufferStorage(newHandle, capacity, 0);
         GL45.glCopyNamedBufferSubData(oldHandle, newHandle, 0L, 0L, this.capacity);
         this.deleteInternal(oldHandle);
         this.handle(newHandle);
      } else {
         GL45.glNamedBufferStorage(this.handle(), capacity, 0);
      }

      this.capacity = capacity;
      FlwMemoryTracker._allocGpuMemory(this.capacity);
   }

   @Override
   protected void deleteInternal(int handle) {
      GL15.glDeleteBuffers(handle);
   }

   @Override
   public void delete() {
      super.delete();
      FlwMemoryTracker._freeGpuMemory(this.capacity);
   }
}
