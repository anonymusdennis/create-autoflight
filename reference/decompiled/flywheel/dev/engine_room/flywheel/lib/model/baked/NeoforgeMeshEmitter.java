package dev.engine_room.flywheel.lib.model.baked;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.engine_room.flywheel.api.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class NeoforgeMeshEmitter extends MeshEmitter implements VertexConsumer {
   private final RenderType renderType;
   private boolean defaultAo;

   NeoforgeMeshEmitter(ByteBufferBuilderStack byteBufferBuilderStack, RenderType renderType) {
      super(byteBufferBuilderStack, renderType);
      this.renderType = renderType;
   }

   public void prepareForModelLayer(boolean defaultAo) {
      this.defaultAo = defaultAo;
   }

   @Nullable
   private BufferBuilder getBuffer(boolean shade, boolean ao) {
      Material key = this.blockMaterialFunction.apply(this.renderType, shade, ao);
      return key != null ? this.getBuffer(key) : null;
   }

   @Nullable
   private BufferBuilder getBuffer(BakedQuad quad) {
      boolean shade = quad.isShade();
      boolean ao = quad.hasAmbientOcclusion() && this.defaultAo;
      return this.getBuffer(shade, ao);
   }

   public void putBulkData(Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
      BufferBuilder bufferBuilder = this.getBuffer(quad);
      if (bufferBuilder != null) {
         bufferBuilder.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay);
      }
   }

   public void putBulkData(
      Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay, boolean readAlpha
   ) {
      BufferBuilder bufferBuilder = this.getBuffer(quad);
      if (bufferBuilder != null) {
         bufferBuilder.putBulkData(pose, quad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readAlpha);
      }
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      throw new UnsupportedOperationException("ForgeMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setColor(int red, int green, int blue, int alpha) {
      throw new UnsupportedOperationException("ForgeMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setUv(float u, float v) {
      throw new UnsupportedOperationException("ForgeMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setUv1(int u, int v) {
      throw new UnsupportedOperationException("ForgeMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setUv2(int u, int v) {
      throw new UnsupportedOperationException("ForgeMeshEmitter only supports putBulkData!");
   }

   public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
      throw new UnsupportedOperationException("ForgeMeshEmitter only supports putBulkData!");
   }
}
