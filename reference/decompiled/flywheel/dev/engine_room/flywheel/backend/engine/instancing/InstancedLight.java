package dev.engine_room.flywheel.backend.engine.instancing;

import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.system.MemoryUtil;

public class InstancedLight {
   private final GlBuffer lut = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
   private final GlBuffer sections = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
   private final TextureBuffer lutTexture = new TextureBuffer(33334);
   private final TextureBuffer sectionsTexture = new TextureBuffer(33334);

   public void bind() {
      Samplers.LIGHT_LUT.makeActive();
      this.lutTexture.bind(this.lut.handle());
      Samplers.LIGHT_SECTIONS.makeActive();
      this.sectionsTexture.bind(this.sections.handle());
   }

   public void flush(LightStorage light) {
      if (light.capacity() != 0) {
         light.upload(this.sections);
         if (light.checkNeedsLutRebuildAndClear()) {
            IntArrayList lut = light.createLut();
            MemoryBlock up = MemoryBlock.malloc((long)lut.size() * 4L);
            long ptr = up.ptr();

            for (int i = 0; i < lut.size(); i++) {
               MemoryUtil.memPutInt(ptr + 4L * (long)i, lut.getInt(i));
            }

            this.lut.upload(up);
            up.free();
         }
      }
   }

   public void delete() {
      this.lut.delete();
      this.sections.delete();
      this.lutTexture.delete();
      this.sectionsTexture.delete();
   }
}
