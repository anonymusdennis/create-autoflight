package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.backend.engine.LightStorage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

public class LightBuffers {
   private final ResizableStorageArray lut = new ResizableStorageArray(4L);
   private final ResizableStorageArray sections = new ResizableStorageArray((long)LightStorage.SECTION_SIZE_BYTES);

   public void flush(StagingBuffer staging, LightStorage light) {
      int capacity = light.capacity();
      if (capacity != 0) {
         this.sections.ensureCapacity((long)capacity);
         light.uploadChangedSections(staging, this.sections.handle());
         if (light.checkNeedsLutRebuildAndClear()) {
            IntArrayList lut = light.createLut();
            this.lut.ensureCapacity((long)lut.size());
            staging.enqueueCopy((long)lut.size() * 4L, this.lut.handle(), 0L, ptr -> {
               for (int i = 0; i < lut.size(); i++) {
                  MemoryUtil.memPutInt(ptr + (long)i * 4L, lut.getInt(i));
               }
            });
         }
      }
   }

   public void bind() {
      if (this.sections.capacity() != 0L) {
         GL46.glBindBufferRange(37074, 5, this.lut.handle(), 0L, this.lut.byteCapacity());
         GL46.glBindBufferRange(37074, 6, this.sections.handle(), 0L, this.sections.byteCapacity());
      }
   }

   public void delete() {
      this.lut.delete();
      this.sections.delete();
   }
}
