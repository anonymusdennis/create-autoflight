package dev.engine_room.flywheel.lib.vertex;

import dev.engine_room.flywheel.lib.math.DataPacker;
import org.lwjgl.system.MemoryUtil;

public class FullVertexView extends AbstractVertexView {
   public static final long STRIDE = 36L;

   @Override
   public long stride() {
      return 36L;
   }

   @Override
   public float x(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 36L);
   }

   @Override
   public float y(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 36L + 4L);
   }

   @Override
   public float z(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 36L + 8L);
   }

   @Override
   public float r(int index) {
      return DataPacker.unpackNormU8(MemoryUtil.memGetByte(this.ptr + (long)index * 36L + 12L));
   }

   @Override
   public float g(int index) {
      return DataPacker.unpackNormU8(MemoryUtil.memGetByte(this.ptr + (long)index * 36L + 13L));
   }

   @Override
   public float b(int index) {
      return DataPacker.unpackNormU8(MemoryUtil.memGetByte(this.ptr + (long)index * 36L + 14L));
   }

   @Override
   public float a(int index) {
      return DataPacker.unpackNormU8(MemoryUtil.memGetByte(this.ptr + (long)index * 36L + 15L));
   }

   @Override
   public float u(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 36L + 16L);
   }

   @Override
   public float v(int index) {
      return MemoryUtil.memGetFloat(this.ptr + (long)index * 36L + 20L);
   }

   @Override
   public int overlay(int index) {
      return MemoryUtil.memGetInt(this.ptr + (long)index * 36L + 24L);
   }

   @Override
   public int light(int index) {
      return MemoryUtil.memGetInt(this.ptr + (long)index * 36L + 28L);
   }

   @Override
   public float normalX(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 36L + 32L));
   }

   @Override
   public float normalY(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 36L + 33L));
   }

   @Override
   public float normalZ(int index) {
      return DataPacker.unpackNormI8(MemoryUtil.memGetByte(this.ptr + (long)index * 36L + 34L));
   }

   @Override
   public void x(int index, float x) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 36L, x);
   }

   @Override
   public void y(int index, float y) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 36L + 4L, y);
   }

   @Override
   public void z(int index, float z) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 36L + 8L, z);
   }

   @Override
   public void r(int index, float r) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 36L + 12L, DataPacker.packNormU8(r));
   }

   @Override
   public void g(int index, float g) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 36L + 13L, DataPacker.packNormU8(g));
   }

   @Override
   public void b(int index, float b) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 36L + 14L, DataPacker.packNormU8(b));
   }

   @Override
   public void a(int index, float a) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 36L + 15L, DataPacker.packNormU8(a));
   }

   @Override
   public void u(int index, float u) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 36L + 16L, u);
   }

   @Override
   public void v(int index, float v) {
      MemoryUtil.memPutFloat(this.ptr + (long)index * 36L + 20L, v);
   }

   @Override
   public void overlay(int index, int overlay) {
      MemoryUtil.memPutInt(this.ptr + (long)index * 36L + 24L, overlay);
   }

   @Override
   public void light(int index, int light) {
      MemoryUtil.memPutInt(this.ptr + (long)index * 36L + 28L, light);
   }

   @Override
   public void normalX(int index, float normalX) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 36L + 32L, DataPacker.packNormI8(normalX));
   }

   @Override
   public void normalY(int index, float normalY) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 36L + 33L, DataPacker.packNormI8(normalY));
   }

   @Override
   public void normalZ(int index, float normalZ) {
      MemoryUtil.memPutByte(this.ptr + (long)index * 36L + 34L, DataPacker.packNormI8(normalZ));
   }
}
