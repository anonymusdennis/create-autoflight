package dev.engine_room.flywheel.backend.gl.array;

import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.GlStateTracker;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import net.minecraft.Util;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Checks;

public class GlVertexArraySeparateAttributes extends GlVertexArray {
   public static final boolean SUPPORTED = isSupported();
   private final BitSet attributeEnabled = new BitSet(MAX_ATTRIBS);
   private final VertexAttribute[] attributes = new VertexAttribute[MAX_ATTRIBS];
   private final int[] attributeBindings = (int[])Util.make(new int[MAX_ATTRIBS], a -> Arrays.fill(a, -1));
   private final int[] bindingBuffers = new int[16];
   private final long[] bindingOffsets = new long[16];
   private final int[] bindingStrides = new int[16];
   private final int[] bindingDivisors = new int[16];
   private int elementBufferBinding = 0;

   public GlVertexArraySeparateAttributes() {
      this.handle(GL43C.glGenVertexArrays());
   }

   @Override
   public void bindVertexBuffer(int bindingIndex, int vbo, long offset, int stride) {
      if (this.bindingBuffers[bindingIndex] != vbo || this.bindingOffsets[bindingIndex] != offset || this.bindingStrides[bindingIndex] != stride) {
         GlStateTracker.bindVao(this.handle());
         GL43C.glBindVertexBuffer(bindingIndex, vbo, offset, stride);
         this.bindingBuffers[bindingIndex] = vbo;
         this.bindingOffsets[bindingIndex] = offset;
         this.bindingStrides[bindingIndex] = stride;
      }
   }

   @Override
   public void setBindingDivisor(int bindingIndex, int divisor) {
      if (this.bindingDivisors[bindingIndex] != divisor) {
         GL43C.glVertexBindingDivisor(bindingIndex, divisor);
         this.bindingDivisors[bindingIndex] = divisor;
      }
   }

   @Override
   public void bindAttributes(int bindingIndex, int startAttribIndex, List<VertexAttribute> vertexAttributes) {
      GlStateTracker.bindVao(this.handle());
      int attribIndex = startAttribIndex;
      int offset = 0;

      for (VertexAttribute attribute : vertexAttributes) {
         if (!this.attributeEnabled.get(attribIndex)) {
            GL43C.glEnableVertexAttribArray(attribIndex);
            this.attributeEnabled.set(attribIndex);
         }

         if (!attribute.equals(this.attributes[attribIndex])) {
            if (attribute instanceof VertexAttribute.Float f) {
               GL43C.glVertexAttribFormat(attribIndex, f.size(), f.type().glEnum, f.normalized(), offset);
            } else if (attribute instanceof VertexAttribute.Int vi) {
               GL43C.glVertexAttribIFormat(attribIndex, vi.size(), vi.type().glEnum, offset);
            }

            this.attributes[attribIndex] = attribute;
         }

         if (this.attributeBindings[attribIndex] != bindingIndex) {
            GL43C.glVertexAttribBinding(attribIndex, bindingIndex);
            this.attributeBindings[attribIndex] = bindingIndex;
         }

         attribIndex++;
         offset += attribute.byteWidth();
      }
   }

   @Override
   public void setElementBuffer(int ebo) {
      if (this.elementBufferBinding != ebo) {
         GlStateTracker.bindVao(this.handle());
         GlBufferType.ELEMENT_ARRAY_BUFFER.bind(ebo);
         this.elementBufferBinding = ebo;
      }
   }

   private static boolean isSupported() {
      GLCapabilities c = GlCompat.CAPABILITIES;
      return Checks.checkFunctions(
         new long[]{
            c.glBindVertexBuffer,
            c.glVertexBindingDivisor,
            c.glEnableVertexAttribArray,
            c.glVertexAttribFormat,
            c.glVertexAttribIFormat,
            c.glVertexAttribBinding
         }
      );
   }
}
