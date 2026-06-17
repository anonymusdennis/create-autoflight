package dev.engine_room.flywheel.backend.gl.array;

import dev.engine_room.flywheel.backend.gl.GlCompat;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import net.minecraft.Util;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Checks;

public class GlVertexArrayDSA extends GlVertexArray {
   public static final boolean SUPPORTED = isSupported();
   private final BitSet attributeEnabled = new BitSet(MAX_ATTRIBS);
   private final VertexAttribute[] attributes = new VertexAttribute[MAX_ATTRIBS];
   private final int[] attributeBindings = (int[])Util.make(new int[MAX_ATTRIBS], a -> Arrays.fill(a, -1));
   private final int[] bindingBuffers = new int[16];
   private final long[] bindingOffsets = new long[16];
   private final int[] bindingStrides = new int[16];
   private final int[] bindingDivisors = new int[16];
   private int elementBufferBinding = 0;

   public GlVertexArrayDSA() {
      this.handle(GL45C.glCreateVertexArrays());
   }

   @Override
   public void bindVertexBuffer(int bindingIndex, int vbo, long offset, int stride) {
      if (this.bindingBuffers[bindingIndex] != vbo || this.bindingOffsets[bindingIndex] != offset || this.bindingStrides[bindingIndex] != stride) {
         GL45C.glVertexArrayVertexBuffer(this.handle(), bindingIndex, vbo, offset, stride);
         this.bindingBuffers[bindingIndex] = vbo;
         this.bindingOffsets[bindingIndex] = offset;
         this.bindingStrides[bindingIndex] = stride;
      }
   }

   @Override
   public void setBindingDivisor(int bindingIndex, int divisor) {
      if (this.bindingDivisors[bindingIndex] != divisor) {
         GL45C.glVertexArrayBindingDivisor(this.handle(), bindingIndex, divisor);
         this.bindingDivisors[bindingIndex] = divisor;
      }
   }

   @Override
   public void bindAttributes(int bindingIndex, int startAttribIndex, List<VertexAttribute> vertexAttributes) {
      int handle = this.handle();
      int attribIndex = startAttribIndex;
      int offset = 0;

      for (VertexAttribute attribute : vertexAttributes) {
         if (!this.attributeEnabled.get(attribIndex)) {
            GL45C.glEnableVertexArrayAttrib(handle, attribIndex);
            this.attributeEnabled.set(attribIndex);
         }

         if (!attribute.equals(this.attributes[attribIndex])) {
            if (attribute instanceof VertexAttribute.Float f) {
               GL45C.glVertexArrayAttribFormat(handle, attribIndex, f.size(), f.type().glEnum, f.normalized(), offset);
            } else if (attribute instanceof VertexAttribute.Int vi) {
               GL45C.glVertexArrayAttribIFormat(handle, attribIndex, vi.size(), vi.type().glEnum, offset);
            }

            this.attributes[attribIndex] = attribute;
         }

         if (this.attributeBindings[attribIndex] != bindingIndex) {
            GL45C.glVertexArrayAttribBinding(handle, attribIndex, bindingIndex);
            this.attributeBindings[attribIndex] = bindingIndex;
         }

         attribIndex++;
         offset += attribute.byteWidth();
      }
   }

   @Override
   public void setElementBuffer(int ebo) {
      if (this.elementBufferBinding != ebo) {
         GL45C.glVertexArrayElementBuffer(this.handle(), ebo);
         this.elementBufferBinding = ebo;
      }
   }

   private static boolean isSupported() {
      GLCapabilities c = GlCompat.CAPABILITIES;
      return Checks.checkFunctions(
         new long[]{
            c.glCreateVertexArrays,
            c.glVertexArrayElementBuffer,
            c.glVertexArrayVertexBuffer,
            c.glVertexArrayBindingDivisor,
            c.glVertexArrayAttribBinding,
            c.glEnableVertexArrayAttrib,
            c.glVertexArrayAttribFormat,
            c.glVertexArrayAttribIFormat
         }
      );
   }
}
