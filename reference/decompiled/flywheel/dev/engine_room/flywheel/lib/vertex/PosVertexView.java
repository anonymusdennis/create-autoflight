package dev.engine_room.flywheel.lib.vertex;

import org.lwjgl.system.MemoryUtil;

public class PosVertexView extends AbstractVertexView implements DefaultVertexList {
   public static final long STRIDE = 12L;

   @Override
   public long stride() {
      return 12L;
   }

   @Override
   public float x(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 12L);
   }

   @Override
   public float y(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 12L + 4L);
   }

   @Override
   public float z(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 12L + 8L);
   }

   @Override
   public void x(int index, float x) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 12L, x);
   }

   @Override
   public void y(int index, float y) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 12L + 4L, y);
   }

   @Override
   public void z(int index, float z) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 12L + 8L, z);
   }
}
