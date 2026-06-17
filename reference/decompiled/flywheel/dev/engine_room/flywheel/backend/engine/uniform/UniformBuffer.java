package dev.engine_room.flywheel.backend.engine.uniform;

import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.lib.math.MoreMath;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32;

public class UniformBuffer {
   private final int index;
   private final MemoryBlock clientBuffer;
   @Nullable
   private GlBuffer buffer;
   private boolean needsUpload;

   public UniformBuffer(int index, int size) {
      this.index = index;
      this.clientBuffer = MemoryBlock.malloc((long)MoreMath.align16(size));
      this.clientBuffer.clear();
   }

   public long ptr() {
      return this.clientBuffer.ptr();
   }

   public void markDirty() {
      this.needsUpload = true;
   }

   public void clear() {
      this.clientBuffer.clear();
      this.markDirty();
   }

   public void bind() {
      if (this.buffer == null) {
         this.buffer = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
         this.needsUpload = true;
      }

      if (this.needsUpload) {
         this.buffer.upload(this.clientBuffer);
         this.needsUpload = false;
      }

      GL32.glBindBufferRange(35345, this.index, this.buffer.handle(), 0L, this.clientBuffer.size());
   }

   public void delete() {
      if (this.buffer != null) {
         this.buffer.delete();
         this.buffer = null;
      }
   }
}
