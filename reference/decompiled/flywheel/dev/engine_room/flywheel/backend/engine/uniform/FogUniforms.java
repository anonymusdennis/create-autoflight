package dev.engine_room.flywheel.backend.engine.uniform;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;

public final class FogUniforms extends UniformWriter {
   private static final int SIZE = 28;
   static final UniformBuffer BUFFER = new UniformBuffer(1, 28);

   public static void update() {
      long ptr = BUFFER.ptr();
      float[] color = RenderSystem.getShaderFogColor();
      ptr = writeFloat(ptr, color[0]);
      ptr = writeFloat(ptr, color[1]);
      ptr = writeFloat(ptr, color[2]);
      ptr = writeFloat(ptr, color[3]);
      ptr = writeFloat(ptr, RenderSystem.getShaderFogStart());
      ptr = writeFloat(ptr, RenderSystem.getShaderFogEnd());
      FogShape fogShape = RenderSystem.getShaderFogShape();
      ptr = writeInt(ptr, (fogShape == null ? FogShape.SPHERE : fogShape).getIndex());
      BUFFER.markDirty();
   }
}
