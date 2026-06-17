package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.backend.util.MemoryBuffer;
import org.lwjgl.system.MemoryUtil;

public class TransferList {
   private static final long STRIDE = 32L;
   private final MemoryBuffer block = new MemoryBuffer(32L);
   private int length;

   public void push(int vbo, long srcOffset, long dstOffset, long size) {
      if (this.continuesLast(vbo, srcOffset, dstOffset)) {
         int lastIndex = this.length - 1;
         this.size(lastIndex, this.size(lastIndex) + size);
      } else {
         this.block.reallocIfNeeded(this.length);
         this.vbo(this.length, vbo);
         this.srcOffset(this.length, srcOffset);
         this.dstOffset(this.length, dstOffset);
         this.size(this.length, size);
         this.length++;
      }
   }

   public int length() {
      return this.length;
   }

   public boolean isEmpty() {
      return this.length == 0;
   }

   public void reset() {
      this.length = 0;
   }

   public int vbo(int index) {
      return MemoryUtil.memGetInt(this.block.ptrForIndex(index));
   }

   public long srcOffset(int index) {
      return MemoryUtil.memGetLong(this.block.ptrForIndex(index) + 8L);
   }

   public long dstOffset(int index) {
      return MemoryUtil.memGetLong(this.block.ptrForIndex(index) + 16L);
   }

   public long size(int index) {
      return MemoryUtil.memGetLong(this.block.ptrForIndex(index) + 24L);
   }

   public void delete() {
      this.block.delete();
   }

   private boolean continuesLast(int vbo, long srcOffset, long dstOffset) {
      if (this.length == 0) {
         return false;
      } else {
         int lastIndex = this.length - 1;
         long lastSize = this.size(lastIndex);
         return this.vbo(lastIndex) == vbo && this.dstOffset(lastIndex) + lastSize == dstOffset && this.srcOffset(lastIndex) + lastSize == srcOffset;
      }
   }

   private void vbo(int index, int vbo) {
      MemoryUtil.memPutInt(this.block.ptrForIndex(index), vbo);
   }

   private void srcOffset(int index, long srcOffset) {
      MemoryUtil.memPutLong(this.block.ptrForIndex(index) + 8L, srcOffset);
   }

   private void dstOffset(int index, long dstOffset) {
      MemoryUtil.memPutLong(this.block.ptrForIndex(index) + 16L, dstOffset);
   }

   private void size(int index, long size) {
      MemoryUtil.memPutLong(this.block.ptrForIndex(index) + 24L, size);
   }
}
