package dev.engine_room.flywheel.backend.gl;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;

public class GlStateTracker {
   private static final int[] BUFFERS = new int[GlBufferType.values().length];
   private static int vao;
   private static int program;

   public static int getBuffer(GlBufferType type) {
      return BUFFERS[type.ordinal()];
   }

   public static int getVertexArray() {
      return vao;
   }

   public static int getProgram() {
      return program;
   }

   public static void _setBuffer(GlBufferType type, int id) {
      BUFFERS[type.ordinal()] = id;
   }

   public static void _setVertexArray(int id) {
      vao = id;
   }

   public static void _setProgram(int id) {
      program = id;
   }

   public static GlStateTracker.State getRestoreState() {
      return new GlStateTracker.State((int[])BUFFERS.clone(), vao, program, GlStateManager._getActiveTexture());
   }

   public static void bindVao(int vao) {
      if (vao != GlStateTracker.vao) {
         GlStateManager._glBindVertexArray(vao);
      }
   }

   public static void bindBuffer(GlBufferType type, int buffer) {
      if (BUFFERS[type.ordinal()] != buffer || type == GlBufferType.ELEMENT_ARRAY_BUFFER) {
         GlStateManager._glBindBuffer(type.glEnum, buffer);
      }
   }

   public static record State(int[] buffers, int vao, int program, int activeTexture) implements AutoCloseable {
      public void restore() {
         if (this.vao != GlStateTracker.vao) {
            GlStateManager._glBindVertexArray(this.vao);
         }

         GlBufferType[] values = GlBufferType.values();

         for (int i = 0; i < values.length; i++) {
            if (this.buffers[i] != GlStateTracker.BUFFERS[i] && values[i] != GlBufferType.ELEMENT_ARRAY_BUFFER) {
               GlStateManager._glBindBuffer(values[i].glEnum, this.buffers[i]);
            }
         }

         if (this.program != GlStateTracker.program) {
            GlStateManager._glUseProgram(this.program);
         }

         GlStateManager._activeTexture(this.activeTexture);
      }

      @Override
      public void close() {
         this.restore();
      }
   }
}
