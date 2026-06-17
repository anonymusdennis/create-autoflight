package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.MeshData;
import java.nio.ByteBuffer;
import net.minecraft.client.renderer.texture.OverlayTexture;

public class MutableTemplateMesh extends TemplateMesh {
   public MutableTemplateMesh() {
      this(0);
   }

   public MutableTemplateMesh(int vertexCount) {
      super(vertexCount);
   }

   public MutableTemplateMesh(TemplateMesh template) {
      super(0);
      this.copyFrom(0, template);
   }

   public MutableTemplateMesh(MeshData data) {
      super(0);
      this.copyFrom(0, data);
   }

   @Deprecated(
      forRemoval = true
   )
   public MutableTemplateMesh(int[] data) {
      super(data);
   }

   @Deprecated(
      forRemoval = true
   )
   public static void transferFromVertexData(int srcIndex, int dstIndex, int vertexCount, MutableTemplateMesh mutableMesh, ByteBuffer vertexBuffer, int stride) {
      mutableMesh.copyFrom(srcIndex, dstIndex, vertexCount, vertexBuffer, stride);
   }

   public void ensureCapacity(int vertexCount) {
      if (vertexCount > this.data.length / 9) {
         int[] newData = new int[vertexCount * 9];
         System.arraycopy(this.data, 0, newData, 0, this.data.length);
         this.data = newData;
      }
   }

   public void copyFrom(int index, TemplateMesh template) {
      if (index >= 0 && index <= this.vertexCount) {
         this.ensureCapacity(index + template.vertexCount);
         this.vertexCount = index + template.vertexCount;
         System.arraycopy(template.data, 0, this.data, index * 9, template.vertexCount * 9);
      } else {
         throw new IllegalArgumentException();
      }
   }

   public void copyFrom(int srcIndex, int dstIndex, int vertexCount, ByteBuffer vertexBuffer, int stride) {
      if (dstIndex >= 0 && dstIndex <= this.vertexCount) {
         this.ensureCapacity(dstIndex + vertexCount);
         this.vertexCount = dstIndex + vertexCount;

         for (int i = 0; i < vertexCount; i++) {
            this.x(dstIndex + i, vertexBuffer.getFloat(srcIndex + i * stride));
            this.y(dstIndex + i, vertexBuffer.getFloat(srcIndex + i * stride + 4));
            this.z(dstIndex + i, vertexBuffer.getFloat(srcIndex + i * stride + 8));
            this.color(dstIndex + i, vertexBuffer.getInt(srcIndex + i * stride + 12));
            this.u(dstIndex + i, vertexBuffer.getFloat(srcIndex + i * stride + 16));
            this.v(dstIndex + i, vertexBuffer.getFloat(srcIndex + i * stride + 20));
            this.overlay(dstIndex + i, OverlayTexture.NO_OVERLAY);
            this.light(dstIndex + i, vertexBuffer.getInt(srcIndex + i * stride + 24));
            this.normal(dstIndex + i, vertexBuffer.getInt(srcIndex + i * stride + 28));
         }
      } else {
         throw new IllegalArgumentException();
      }
   }

   public void copyFrom(int index, MeshData data) {
      int vertexCount = data.drawState().vertexCount();
      ByteBuffer vertexBuffer = data.vertexBuffer();
      int stride = data.drawState().format().getVertexSize();
      this.copyFrom(0, index, vertexCount, vertexBuffer, stride);
   }

   public void x(int index, float x) {
      this.data[index * 9 + 0] = Float.floatToRawIntBits(x);
   }

   public void y(int index, float y) {
      this.data[index * 9 + 1] = Float.floatToRawIntBits(y);
   }

   public void z(int index, float z) {
      this.data[index * 9 + 2] = Float.floatToRawIntBits(z);
   }

   public void color(int index, int color) {
      this.data[index * 9 + 3] = color;
   }

   public void u(int index, float u) {
      this.data[index * 9 + 4] = Float.floatToRawIntBits(u);
   }

   public void v(int index, float v) {
      this.data[index * 9 + 5] = Float.floatToRawIntBits(v);
   }

   public void overlay(int index, int overlay) {
      this.data[index * 9 + 6] = overlay;
   }

   public void light(int index, int light) {
      this.data[index * 9 + 7] = light;
   }

   public void normal(int index, int normal) {
      this.data[index * 9 + 8] = normal;
   }

   public TemplateMesh toImmutable() {
      int[] newData = new int[this.vertexCount * 9];
      System.arraycopy(this.data, 0, newData, 0, newData.length);
      return new TemplateMesh(newData);
   }

   public void clear() {
      this.vertexCount = 0;
   }
}
