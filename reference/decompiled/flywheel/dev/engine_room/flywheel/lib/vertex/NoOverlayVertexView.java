package dev.engine_room.flywheel.lib.vertex;

import dev.engine_room.flywheel.lib.math.DataPacker;
import org.lwjgl.system.MemoryUtil;

public class NoOverlayVertexView extends AbstractVertexView implements DefaultVertexList {
   public static final long STRIDE = 31L;

   @Override
   public long stride() {
      return 31L;
   }

   @Override
   public float x(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 31L);
   }

   @Override
   public float y(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 31L + 4L);
   }

   @Override
   public float z(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 31L + 8L);
   }

   @Override
   public float r(int index) {
      return DataPacker.unpackNormU8(MemoryUtil.memGetByte(this.ptr + (long)index * 31L + 12L));
   }

   @Override
   public float g(int index) {
      return DataPacker.unpackNormU8(MemoryUtil.memGetByte(this.ptr + (long)index * 31L + 13L));
   }

   @Override
   public float b(int index) {
      return DataPacker.unpackNormU8(MemoryUtil.memGetByte(this.ptr + (long)index * 31L + 14L));
   }

   @Override
   public float a(int index) {
      return DataPacker.unpackNormU8(MemoryUtil.memGetByte(this.ptr + (long)index * 31L + 15L));
   }

   @Override
   public float u(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 31L + 16L);
   }

   @Override
   public float v(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 31L + 20L);
   }

   @Override
   public int light(int index) {
      return MemoryUtil.memGetInt(this.ptr + (long)index * 31L + 24L);
   }

   @Override
   public float normalX(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 31L + 28L));
   }

   @Override
   public float normalY(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 31L + 29L));
   }

   @Override
   public float normalZ(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 31L + 30L));
   }

   @Override
   public void x(int index, float x) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 31L, x);
   }

   @Override
   public void y(int index, float y) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 31L + 4L, y);
   }

   @Override
   public void z(int index, float z) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 31L + 8L, z);
   }

   @Override
   public void r(int index, float r) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 31L + 12L, DataPacker.packNormU8(r));
   }

   @Override
   public void g(int index, float g) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 31L + 13L, DataPacker.packNormU8(g));
   }

   @Override
   public void b(int index, float b) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 31L + 14L, DataPacker.packNormU8(b));
   }

   @Override
   public void a(int index, float a) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 31L + 15L, DataPacker.packNormU8(a));
   }

   @Override
   public void u(int index, float u) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 31L + 16L, u);
   }

   @Override
   public void v(int index, float v) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 31L + 20L, v);
   }

   @Override
   public void light(int index, int light) {
      MemoryUtil.memPutInt(this.ptr + (long)index * 31L + 24L, light);
   }

   @Override
   public void normalX(int index, float normalX) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 31L + 28L, DataPacker.packNormI8(normalX));
   }

   @Override
   public void normalY(int index, float normalY) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 31L + 29L, DataPacker.packNormI8(normalY));
   }

   @Override
   public void normalZ(int index, float normalZ) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 31L + 30L, DataPacker.packNormI8(normalZ));
   }
}
