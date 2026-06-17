package dev.engine_room.flywheel.lib.vertex;

import dev.engine_room.flywheel.lib.math.DataPacker;
import org.lwjgl.system.MemoryUtil;

public class PosTexNormalVertexView extends AbstractVertexView implements DefaultVertexList {
   public static final long STRIDE = 23L;

   @Override
   public long stride() {
      return 23L;
   }

   @Override
   public float x(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 23L);
   }

   @Override
   public float y(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 23L + 4L);
   }

   @Override
   public float z(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 23L + 8L);
   }

   @Override
   public float u(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 23L + 12L);
   }

   @Override
   public float v(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 23L + 16L);
   }

   @Override
   public float normalX(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 23L + 20L));
   }

   @Override
   public float normalY(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 23L + 21L));
   }

   @Override
   public float normalZ(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 23L + 22L));
   }

   @Override
   public void x(int index, float x) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 23L, x);
   }

   @Override
   public void y(int index, float y) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 23L + 4L, y);
   }

   @Override
   public void z(int index, float z) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 23L + 8L, z);
   }

   @Override
   public void u(int index, float u) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 23L + 12L, u);
   }

   @Override
   public void v(int index, float v) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 23L + 16L, v);
   }

   @Override
   public void normalX(int index, float normalX) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 23L + 20L, DataPacker.packNormI8(normalX));
   }

   @Override
   public void normalY(int index, float normalY) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 23L + 21L, DataPacker.packNormI8(normalY));
   }

   @Override
   public void normalZ(int index, float normalZ) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 23L + 22L, DataPacker.packNormI8(normalZ));
   }
}
