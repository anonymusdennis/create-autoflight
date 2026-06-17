package net.createmod.catnip.impl.client.render.model;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.createmod.catnip.client.render.model.ShadeSeparatedResultConsumer;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.UnknownNullability;

class MeshEmitter {
   private final RenderType renderType;
   private final ByteBufferBuilder byteBufferBuilder;
   @UnknownNullability
   private BufferBuilder bufferBuilder;
   @UnknownNullability
   private ShadeSeparatedResultConsumer resultConsumer;
   private boolean currentShade;

   MeshEmitter(RenderType renderType) {
      this.renderType = renderType;
      this.byteBufferBuilder = new ByteBufferBuilder(renderType.bufferSize());
   }

   public void prepare(ShadeSeparatedResultConsumer resultConsumer) {
      this.resultConsumer = resultConsumer;
   }

   public void end() {
      if (this.bufferBuilder != null) {
         this.emit();
      }

      this.resultConsumer = null;
   }

   public BufferBuilder getBuffer(boolean shade) {
      this.prepareForGeometry(shade);
      return this.bufferBuilder;
   }

   private void prepareForGeometry(boolean shade) {
      if (this.bufferBuilder == null) {
         this.bufferBuilder = new BufferBuilder(this.byteBufferBuilder, Mode.QUADS, DefaultVertexFormat.BLOCK);
      } else if (shade != this.currentShade) {
         this.emit();
         this.bufferBuilder = new BufferBuilder(this.byteBufferBuilder, Mode.QUADS, DefaultVertexFormat.BLOCK);
      }

      this.currentShade = shade;
   }

   private void emit() {
      MeshData data = this.bufferBuilder.build();
      this.bufferBuilder = null;
      if (data != null) {
         this.resultConsumer.accept(this.renderType, this.currentShade, data);
         data.close();
      }
   }
}
