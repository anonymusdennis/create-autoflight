package dev.ryanhcode.sable.sublevel.render.staging;

import foundry.veil.api.client.render.VeilRenderSystem;
import java.util.function.LongFunction;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.NativeResource;

public abstract class StagingBuffer implements NativeResource {
   private static StagingBuffer.StagingBufferType stagingBufferType;

   public static StagingBuffer create() {
      return create(16777216L);
   }

   public static StagingBuffer create(long size) {
      if (stagingBufferType == null) {
         GLCapabilities caps = GL.getCapabilities();
         if (!caps.OpenGL44 && !caps.GL_ARB_buffer_storage) {
            stagingBufferType = StagingBuffer.StagingBufferType.LEGACY;
         } else {
            stagingBufferType = VeilRenderSystem.directStateAccessSupported() ? StagingBuffer.StagingBufferType.DSA : StagingBuffer.StagingBufferType.ARB;
         }
      }

      return stagingBufferType.factory.apply(size);
   }

   public abstract void updateFencedAreas();

   public abstract long reserve(long var1);

   public abstract void copy(int var1, long var2);

   public abstract long getSize();

   public abstract long getUsedSize();

   private static enum StagingBufferType {
      LEGACY(DSAStagingBuffer::new),
      ARB(DSAStagingBuffer::new),
      DSA(DSAStagingBuffer::new);

      private final LongFunction<StagingBuffer> factory;

      private StagingBufferType(final LongFunction<StagingBuffer> factory) {
         this.factory = factory;
      }
   }
}
