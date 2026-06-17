package net.createmod.catnip.impl.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public record ColoringVertexConsumer(VertexConsumer delegate, float red, float green, float blue, float alpha) implements VertexConsumer {
   public VertexConsumer addVertex(float x, float y, float z) {
      this.delegate.addVertex(x, y, z);
      return this;
   }

   public VertexConsumer setColor(int r, int g, int b, int a) {
      this.delegate.setColor((int)((float)r * this.red), (int)((float)g * this.green), (int)((float)b * this.blue), (int)((float)a * this.alpha));
      return this;
   }

   public VertexConsumer setUv(float u, float v) {
      this.delegate.setUv(u, v);
      return this;
   }

   public VertexConsumer setUv1(int u, int v) {
      this.delegate.setUv1(u, v);
      return this;
   }

   public VertexConsumer setUv2(int u, int v) {
      this.delegate.setUv2(u, v);
      return this;
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      this.delegate.setNormal(x, y, z);
      return this;
   }
}
