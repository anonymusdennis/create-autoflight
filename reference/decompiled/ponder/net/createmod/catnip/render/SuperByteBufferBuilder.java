package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.MeshData;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class SuperByteBufferBuilder {
   protected final MutableTemplateMesh mesh = new MutableTemplateMesh();
   protected final IntList shadeSwapVertices = new IntArrayList();
   protected boolean currentShade;

   public void prepare() {
      this.mesh.clear();
      this.shadeSwapVertices.clear();
      this.currentShade = true;
   }

   public void add(MeshData data, boolean shaded) {
      if (shaded != this.currentShade) {
         this.shadeSwapVertices.add(this.mesh.vertexCount());
         this.currentShade = shaded;
      }

      this.mesh.copyFrom(this.mesh.vertexCount(), data);
   }

   public SuperByteBuffer build() {
      return new ShadeSeparatingSuperByteBuffer(this.mesh.toImmutable(), this.shadeSwapVertices.toIntArray());
   }
}
