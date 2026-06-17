package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.createmod.ponder.mixin.client.accessor.BufferBuilderAccessor;
import net.minecraft.client.renderer.block.model.BakedQuad;

@Deprecated(
   forRemoval = true
)
public class ShadedBlockSbbBuilder implements VertexConsumer {
   protected static final ByteBufferBuilder BYTE_BUFFER_BUILDER = new ByteBufferBuilder(512);
   protected BufferBuilder bufferBuilder;
   protected final IntList shadeSwapVertices = new IntArrayList();
   protected boolean currentShade;
   protected boolean invertFakeNormal;

   public static ShadedBlockSbbBuilder create() {
      return new ShadedBlockSbbBuilder();
   }

   public static ShadedBlockSbbBuilder createForPonder() {
      ShadedBlockSbbBuilder builder = new ShadedBlockSbbBuilder();
      builder.invertFakeNormal = true;
      return builder;
   }

   public void begin() {
      this.bufferBuilder = new BufferBuilder(BYTE_BUFFER_BUILDER, Mode.QUADS, DefaultVertexFormat.BLOCK);
      this.shadeSwapVertices.clear();
      this.currentShade = true;
   }

   public SuperByteBuffer end() {
      MeshData data = this.bufferBuilder.build();
      TemplateMesh mesh;
      if (data != null) {
         mesh = new MutableTemplateMesh(data).toImmutable();
         data.close();
      } else {
         mesh = new TemplateMesh(0);
      }

      return new ShadeSeparatingSuperByteBuffer(mesh, this.shadeSwapVertices.toIntArray(), this.invertFakeNormal);
   }

   public BufferBuilder unwrap(boolean shade) {
      this.prepareForGeometry(shade);
      return this.bufferBuilder;
   }

   private void prepareForGeometry(boolean shade) {
      if (shade != this.currentShade) {
         this.shadeSwapVertices.add(((BufferBuilderAccessor)this.bufferBuilder).catnip$getVertices());
         this.currentShade = shade;
      }
   }

   protected void prepareForGeometry(BakedQuad quad) {
      this.prepareForGeometry(quad.isShade());
   }

   public void putBulkData(Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
      this.prepareForGeometry(quad);
      this.bufferBuilder.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay);
   }

   public void putBulkData(
      Pose pose, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay, boolean readExistingColor
   ) {
      this.prepareForGeometry(quad);
      this.bufferBuilder.putBulkData(pose, quad, brightnesses, red, green, blue, alpha, lights, overlay, readExistingColor);
   }

   public VertexConsumer addVertex(float x, float y, float z) {
      throw new UnsupportedOperationException("ShadedBlockSbbBuilder only supports putBulkData!");
   }

   public VertexConsumer setColor(int red, int green, int blue, int alpha) {
      throw new UnsupportedOperationException("ShadedBlockSbbBuilder only supports putBulkData!");
   }

   public VertexConsumer setUv(float u, float v) {
      throw new UnsupportedOperationException("ShadedBlockSbbBuilder only supports putBulkData!");
   }

   public VertexConsumer setUv1(int u, int v) {
      throw new UnsupportedOperationException("ShadedBlockSbbBuilder only supports putBulkData!");
   }

   public VertexConsumer setUv2(int u, int v) {
      throw new UnsupportedOperationException("ShadedBlockSbbBuilder only supports putBulkData!");
   }

   public VertexConsumer setNormal(float x, float y, float z) {
      throw new UnsupportedOperationException("ShadedBlockSbbBuilder only supports putBulkData!");
   }
}
