package dev.engine_room.flywheel.backend.gl;

import org.lwjgl.opengl.GL32;

public class TextureBuffer extends GlObject {
   public static final int MAX_TEXELS = GL32.glGetInteger(35883);
   public static final int MAX_BYTES = MAX_TEXELS * 16;
   private final int format;

   public TextureBuffer() {
      this(36208);
   }

   public TextureBuffer(int format) {
      this.handle(GL32.glGenTextures());
      this.format = format;
   }

   public void bind(int buffer) {
      GL32.glBindTexture(35882, this.handle());
      GL32.glTexBuffer(35882, this.format, buffer);
   }

   @Override
   protected void deleteInternal(int handle) {
      GL32.glDeleteTextures(handle);
   }
}
