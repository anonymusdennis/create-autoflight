package dev.engine_room.flywheel.backend.gl.array;

import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import net.minecraft.Util;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.system.Checks;

public abstract class GlVertexArrayGL3 extends GlVertexArray {
   private final BitSet attributeDirty = new BitSet(MAX_ATTRIBS);
   private final int[] attributeOffsets = new int[MAX_ATTRIBS];
   private final VertexAttribute[] attributes = new VertexAttribute[MAX_ATTRIBS];
   private final int[] attributeBindings = (int[])Util.make(new int[MAX_ATTRIBS], a -> Arrays.fill(a, -1));
   private final int[] bindingBuffers = new int[16];
   private final long[] bindingOffsets = new long[16];
   private final int[] bindingStrides = new int[16];
   private final int[] bindingDivisors = new int[16];
   private int requestedElementBuffer = 0;
   private int boundElementBuffer = 0;

   public GlVertexArrayGL3() {
      this.handle(GL30.glGenVertexArrays());
   }

   @Override
   public void bindForDraw() {
      super.bindForDraw();
      this.maybeUpdateAttributes();
      this.maybeUpdateEBOBinding();
   }

   @Override
   public void bindVertexBuffer(int bindingIndex, int vbo, long offset, int stride) {
      if (this.bindingBuffers[bindingIndex] != vbo || this.bindingOffsets[bindingIndex] != offset || this.bindingStrides[bindingIndex] != stride) {
         this.bindingBuffers[bindingIndex] = vbo;
         this.bindingOffsets[bindingIndex] = offset;
         this.bindingStrides[bindingIndex] = stride;

         for (int attribIndex = 0; attribIndex < this.attributeBindings.length; attribIndex++) {
            if (this.attributeBindings[attribIndex] == bindingIndex) {
               this.attributeDirty.set(attribIndex);
            }
         }
      }
   }

   @Override
   public void setBindingDivisor(int bindingIndex, int divisor) {
      if (this.bindingDivisors[bindingIndex] != divisor) {
         this.bindingDivisors[bindingIndex] = divisor;
      }
   }

   @Override
   public void bindAttributes(int bindingIndex, int startAttribIndex, List<VertexAttribute> vertexAttributes) {
      int attribIndex = startAttribIndex;
      int offset = 0;

      for (VertexAttribute attribute : vertexAttributes) {
         this.attributeBindings[attribIndex] = bindingIndex;
         this.attributes[attribIndex] = attribute;
         this.attributeOffsets[attribIndex] = offset;
         this.attributeDirty.set(attribIndex);
         attribIndex++;
         offset += attribute.byteWidth();
      }
   }

   @Override
   public void setElementBuffer(int ebo) {
      this.requestedElementBuffer = ebo;
   }

   private void maybeUpdateEBOBinding() {
      if (this.requestedElementBuffer != this.boundElementBuffer) {
         GlBufferType.ELEMENT_ARRAY_BUFFER.bind(this.requestedElementBuffer);
         this.boundElementBuffer = this.requestedElementBuffer;
      }
   }

   private void maybeUpdateAttributes() {
      for (int attribIndex = this.attributeDirty.nextSetBit(0);
         attribIndex < 16 && attribIndex >= 0;
         attribIndex = this.attributeDirty.nextSetBit(attribIndex + 1)
      ) {
         this.updateAttribute(attribIndex);
      }

      this.attributeDirty.clear();
   }

   private void updateAttribute(int attribIndex) {
      int bindingIndex = this.attributeBindings[attribIndex];
      VertexAttribute attribute = this.attributes[attribIndex];
      if (bindingIndex != -1 && attribute != null) {
         GlBufferType.ARRAY_BUFFER.bind(this.bindingBuffers[bindingIndex]);
         GL20C.glEnableVertexAttribArray(attribIndex);
         long offset = this.bindingOffsets[bindingIndex] + (long)this.attributeOffsets[attribIndex];
         int stride = this.bindingStrides[bindingIndex];
         if (attribute instanceof VertexAttribute.Float f) {
            GL32.glVertexAttribPointer(attribIndex, f.size(), f.type().glEnum(), f.normalized(), stride, offset);
         } else if (attribute instanceof VertexAttribute.Int vi) {
            GL32.glVertexAttribIPointer(attribIndex, vi.size(), vi.type().glEnum(), stride, offset);
         }

         int divisor = this.bindingDivisors[bindingIndex];
         if (divisor != 0) {
            this.setDivisor(attribIndex, divisor);
         }
      }
   }

   protected abstract void setDivisor(int var1, int var2);

   public static class ARB extends GlVertexArrayGL3 {
      public static final boolean SUPPORTED = isSupported();

      @Override
      protected void setDivisor(int attribIndex, int divisor) {
         ARBInstancedArrays.glVertexAttribDivisorARB(attribIndex, divisor);
      }

      private static boolean isSupported() {
         return Checks.checkFunctions(new long[]{GlCompat.CAPABILITIES.glVertexAttribDivisorARB});
      }
   }

   public static class Core extends GlVertexArrayGL3 {
      @Override
      protected void setDivisor(int attribIndex, int divisor) {
         throw new UnsupportedOperationException("Instanced arrays are not supported");
      }

      @Override
      public void setBindingDivisor(int bindingIndex, int divisor) {
         throw new UnsupportedOperationException("Instanced arrays are not supported");
      }
   }

   public static class Core33 extends GlVertexArrayGL3 {
      public static final boolean SUPPORTED = isSupported();

      @Override
      protected void setDivisor(int attribIndex, int divisor) {
         GL33C.glVertexAttribDivisor(attribIndex, divisor);
      }

      private static boolean isSupported() {
         return Checks.checkFunctions(new long[]{GlCompat.CAPABILITIES.glVertexAttribDivisor});
      }
   }
}
