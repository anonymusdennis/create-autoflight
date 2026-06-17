package dev.engine_room.flywheel.lib.model.part;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.math.DataPacker;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import org.lwjgl.system.MemoryUtil;

class VertexWriter implements VertexConsumer {
   private static final int STRIDE = 23;
   private MemoryBlock data;
   private int vertexCount;
   private boolean filledTexture = true;
   private boolean filledNormal = true;

   public VertexWriter() {
      this.data = MemoryBlock.malloc(2944L);
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      this.endLastVertex();
      this.vertexCount++;
      long byteSize = (long)(this.vertexCount * 23);
      long capacity = this.data.size();
      if (byteSize > capacity) {
         this.data = this.data.realloc(capacity * 2L);
      }

      this.filledTexture = false;
      this.filledNormal = false;
      long ptr = this.vertexPtr();
      MemoryUtil.memPutFloat(ptr, x);
      MemoryUtil.memPutFloat(ptr + 4L, y);
      MemoryUtil.memPutFloat(ptr + 8L, z);
      return this;
   }

   public VertexConsumer setColor(int red, int green, int blue, int alpha) {
      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      if (!this.filledTexture) {
         long ptr = this.vertexPtr();
         MemoryUtil.memPutFloat(ptr + 12L, u);
         MemoryUtil.memPutFloat(ptr + 16L, v);
         this.filledTexture = true;
      }

      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      return this;
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      if (!this.filledNormal) {
         long ptr = this.vertexPtr();
         MemoryUtil.memPutByte(ptr + 20L, DataPacker.packNormI8(x));
         MemoryUtil.memPutByte(ptr + 21L, DataPacker.packNormI8(y));
         MemoryUtil.memPutByte(ptr + 22L, DataPacker.packNormI8(z));
         this.filledNormal = true;
      }

      return this;
   }

   private long vertexPtr() {
      return this.data.ptr() + (long)((this.vertexCount - 1) * 23);
   }

   private void endLastVertex() {
      if (this.vertexCount != 0 && (!this.filledTexture || !this.filledNormal)) {
         throw new IllegalStateException("Missing elements in vertex");
      }
   }

   public MemoryBlock copyDataAndReset() {
      this.endLastVertex();
      MemoryBlock dataCopy = MemoryBlock.mallocTracked((long)(this.vertexCount * 23));
      this.data.copyTo(dataCopy);
      this.vertexCount = 0;
      this.filledTexture = true;
      this.filledNormal = true;
      return dataCopy;
   }
}
