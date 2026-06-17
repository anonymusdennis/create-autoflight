package dev.ryanhcode.sable.render.dynamic_shade;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.renderer.block.model.BakedQuad;

public class SubLevelVertexConsumer implements VertexConsumer {
   private final VertexConsumer delegate;
   private boolean verticalNormal;

   public SubLevelVertexConsumer(VertexConsumer delegate) {
      this.delegate = delegate;
   }

   public VertexConsumer addVertex(float f, float g, float h) {
      this.delegate.addVertex(f, g, h);
      return this;
   }

   public VertexConsumer setColor(int i, int j, int k, int l) {
      this.delegate.setColor(i, j, k, l);
      return this;
   }

   public VertexConsumer setUv(float f, float g) {
      this.delegate.setUv(f, g);
      return this;
   }

   public VertexConsumer setUv1(int i, int j) {
      this.delegate.setUv1(i, j);
      return this;
   }

   public VertexConsumer setUv2(int i, int j) {
      this.delegate.setUv2(i, j);
      return this;
   }

   public VertexConsumer setNormal(float pX, float pY, float pZ) {
      if (this.verticalNormal) {
         this.delegate.setNormal(0.0F, 1.0F, 0.0F);
      } else {
         this.delegate.setNormal(pX, pY, pZ);
      }

      return this;
   }

   public void putBulkData(Pose pose, BakedQuad bakedQuad, float[] fs, float f, float g, float h, float i, int[] is, int j, boolean bl) {
      this.verticalNormal = !bakedQuad.isShade();
      super.putBulkData(pose, bakedQuad, fs, f, g, h, i, is, j, bl);
      this.verticalNormal = false;
   }
}
