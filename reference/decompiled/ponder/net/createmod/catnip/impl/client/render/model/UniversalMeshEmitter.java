package net.createmod.catnip.impl.client.render.model;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.createmod.catnip.client.render.model.ShadeSeparatedBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.jetbrains.annotations.UnknownNullability;

class UniversalMeshEmitter implements VertexConsumer {
   @UnknownNullability
   private ShadeSeparatedBufferSource bufferSource;
   @UnknownNullability
   private RenderType layer;

   public void prepare(ShadeSeparatedBufferSource bufferSource, RenderType layer) {
      this.bufferSource = bufferSource;
      this.layer = layer;
   }

   public void clear() {
      this.bufferSource = null;
   }

   public void putBulkData(Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay) {
      VertexConsumer buffer = this.bufferSource.getBuffer(this.layer, quad.isShade());
      buffer.putBulkData(pose, quad, red, green, blue, alpha, light, overlay);
   }

   public void putBulkData(Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay, boolean readExistingColor) {
      VertexConsumer buffer = this.bufferSource.getBuffer(this.layer, quad.isShade());
      buffer.putBulkData(pose, quad, red, green, blue, alpha, light, overlay, readExistingColor);
   }

   public void putBulkData(
      Pose pose, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay, boolean readExistingColor
   ) {
      VertexConsumer buffer = this.bufferSource.getBuffer(this.layer, quad.isShade());
      buffer.putBulkData(pose, quad, brightnesses, red, green, blue, alpha, lights, overlay, readExistingColor);
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      throw new UnsupportedOperationException("UniversalMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setColor(int red, int green, int blue, int alpha) {
      throw new UnsupportedOperationException("UniversalMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setUv(float u, float v) {
      throw new UnsupportedOperationException("UniversalMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setUv1(int u, int v) {
      throw new UnsupportedOperationException("UniversalMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setUv2(int u, int v) {
      throw new UnsupportedOperationException("UniversalMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      throw new UnsupportedOperationException("UniversalMeshEmitter only supports putBulkData!");
   }
}
