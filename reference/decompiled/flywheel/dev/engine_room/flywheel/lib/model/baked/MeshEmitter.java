package dev.engine_room.flywheel.lib.model.baked;

import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import java.util.Arrays;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.UnknownNullability;

class MeshEmitter {
   private static final int INITIAL_CAPACITY = 1;
   private final ByteBufferBuilderStack byteBufferBuilderStack;
   private final RenderType renderType;
   @UnknownNullability
   private Material[] materials = new Material[1];
   @UnknownNullability
   private BufferBuilder[] bufferBuilders = new BufferBuilder[1];
   private int numBufferBuildersPopulated = 0;
   @UnknownNullability
   BlockMaterialFunction blockMaterialFunction;
   private int currentIndex = 0;

   MeshEmitter(ByteBufferBuilderStack byteBufferBuilderStack, RenderType renderType) {
      this.byteBufferBuilderStack = byteBufferBuilderStack;
      this.renderType = renderType;
   }

   public void prepare(BlockMaterialFunction blockMaterialFunction) {
      this.blockMaterialFunction = blockMaterialFunction;
   }

   public void prepareForBlock() {
      this.currentIndex = 0;
   }

   public void end(Builder<Model.ConfiguredMesh> out) {
      for (int index = 0; index < this.numBufferBuildersPopulated; index++) {
         MeshData renderedBuffer = this.bufferBuilders[index].build();
         if (renderedBuffer != null) {
            Material material = this.materials[index];
            Mesh mesh = MeshHelper.blockVerticesToMesh(renderedBuffer, "source=ModelBuilder,material=" + material);
            out.add(new Model.ConfiguredMesh(material, mesh));
            renderedBuffer.close();
         }
      }

      Arrays.fill(this.bufferBuilders, 0, this.numBufferBuildersPopulated, null);
      Arrays.fill(this.materials, 0, this.numBufferBuildersPopulated, null);
      this.currentIndex = 0;
      this.numBufferBuildersPopulated = 0;
      this.blockMaterialFunction = null;
   }

   public BufferBuilder getBuffer(Material material) {
      while (this.currentIndex < this.numBufferBuildersPopulated) {
         if (material.equals(this.materials[this.currentIndex])) {
            return this.bufferBuilders[this.currentIndex];
         }

         this.currentIndex++;
      }

      if (this.currentIndex >= this.materials.length) {
         this.resize(this.materials.length * 2);
      }

      ByteBufferBuilder byteBufferBuilder = this.byteBufferBuilderStack.nextOrCreate();
      BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, this.renderType.mode(), this.renderType.format());
      this.materials[this.currentIndex] = material;
      this.bufferBuilders[this.currentIndex] = bufferBuilder;
      this.numBufferBuildersPopulated++;
      return bufferBuilder;
   }

   private void resize(int capacity) {
      BufferBuilder[] newBufferBuilders = new BufferBuilder[capacity];
      Material[] newMaterials = new Material[capacity];
      System.arraycopy(this.bufferBuilders, 0, newBufferBuilders, 0, this.numBufferBuildersPopulated);
      System.arraycopy(this.materials, 0, newMaterials, 0, this.numBufferBuildersPopulated);
      this.bufferBuilders = newBufferBuilders;
      this.materials = newMaterials;
   }
}
