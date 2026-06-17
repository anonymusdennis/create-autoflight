package dev.engine_room.flywheel.backend.gl.buffer;

import dev.engine_room.flywheel.backend.gl.GlCompat;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Checks;

public interface Buffer {
   Buffer IMPL = new Buffer.DSA().fallback();

   int create();

   void data(int var1, long var2, long var4, int var6);

   void subData(int var1, long var2, long var4, long var6);

   public static class Core implements Buffer {
      @Override
      public int create() {
         return GL15.glGenBuffers();
      }

      @Override
      public void data(int vbo, long size, long ptr, int glEnum) {
         GlBufferType.COPY_WRITE_BUFFER.bind(vbo);
         GL15.nglBufferData(GlBufferType.COPY_WRITE_BUFFER.glEnum, size, ptr, glEnum);
      }

      @Override
      public void subData(int vbo, long offset, long size, long ptr) {
         GlBufferType.COPY_WRITE_BUFFER.bind(vbo);
         GL15.nglBufferSubData(GlBufferType.COPY_WRITE_BUFFER.glEnum, offset, size, ptr);
      }
   }

   public static class DSA implements Buffer {
      @Override
      public int create() {
         return GL45C.glCreateBuffers();
      }

      @Override
      public void data(int vbo, long size, long ptr, int glEnum) {
         GL45C.nglNamedBufferData(vbo, size, ptr, glEnum);
      }

      @Override
      public void subData(int vbo, long offset, long size, long ptr) {
         GL45C.nglNamedBufferSubData(vbo, offset, size, ptr);
      }

      public Buffer fallback() {
         return (Buffer)(dsaMethodsAvailable() ? this : new Buffer.Core());
      }

      private static boolean dsaMethodsAvailable() {
         GLCapabilities c = GlCompat.CAPABILITIES;
         return Checks.checkFunctions(
            new long[]{c.glCreateBuffers, c.glNamedBufferData, c.glCopyNamedBufferSubData, c.glMapNamedBufferRange, c.glUnmapNamedBuffer}
         );
      }
   }
}
