package dev.engine_room.flywheel.backend.engine.instancing;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceWriter;
import dev.engine_room.flywheel.api.layout.Layout;
import dev.engine_room.flywheel.backend.engine.AbstractInstancer;
import dev.engine_room.flywheel.backend.engine.BaseInstancer;
import dev.engine_room.flywheel.backend.engine.InstanceHandleImpl;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.lib.math.MoreMath;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class InstancedInstancer<I extends Instance> extends BaseInstancer<I> {
   private final int instanceStride;
   private final InstanceWriter<I> writer;
   @Nullable
   private GlBuffer vbo;
   private final List<InstancedDraw> draws = new ArrayList<>();

   public InstancedInstancer(InstancerKey<I> key, AbstractInstancer.Recreate<I> recreate) {
      super(key, recreate);
      Layout layout = this.type.layout();
      this.instanceStride = MoreMath.align16(layout.byteSize());
      this.writer = this.type.writer();
   }

   public List<InstancedDraw> draws() {
      return this.draws;
   }

   public void init() {
      if (this.vbo == null) {
         this.vbo = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
      }
   }

   public void updateBuffer() {
      if (!this.changed.isEmpty() && this.vbo != null) {
         int byteSize = this.instanceStride * this.instances.size();
         if (this.needsToGrow((long)byteSize)) {
            MemoryBlock temp = MemoryBlock.malloc(this.increaseSize((long)byteSize));
            this.writeAll(temp.ptr());
            this.vbo.upload(temp);
            temp.free();
         } else {
            this.writeChanged();
         }

         this.changed.clear();
      }
   }

   private void writeChanged() {
      this.changed.forEachSetSpan((startInclusive, endInclusive) -> {
         if (startInclusive < this.instances.size()) {
            int actualEnd = Math.min(endInclusive, this.instances.size() - 1);
            MemoryBlock temp = MemoryBlock.malloc((long)this.instanceStride * (long)(actualEnd - startInclusive + 1));
            long ptr = temp.ptr();

            for (int i = startInclusive; i <= actualEnd; i++) {
               this.writer.write(ptr, this.instances.get(i));
               ptr += (long)this.instanceStride;
            }

            this.vbo.uploadSpan((long)startInclusive * (long)this.instanceStride, temp);
            temp.free();
         }
      });
   }

   private void writeAll(long ptr) {
      for (I instance : this.instances) {
         this.writer.write(ptr, instance);
         ptr += (long)this.instanceStride;
      }
   }

   private long increaseSize(long capacity) {
      return Math.max(capacity + (long)this.instanceStride * 16L, (long)((double)capacity * 1.6));
   }

   public boolean needsToGrow(long capacity) {
      if (capacity < 0L) {
         throw new IllegalArgumentException("Size " + capacity + " < 0");
      } else {
         return capacity == 0L ? false : capacity > this.vbo.size();
      }
   }

   @Override
   public void parallelUpdate() {
      if (!this.deleted.isEmpty()) {
         int oldSize = this.instances.size();
         int removeCount = this.deleted.cardinality();
         if (oldSize == removeCount) {
            this.clear();
         } else {
            int newSize = oldSize - removeCount;
            int writePos = this.deleted.nextSetBit(0);
            if (writePos < newSize) {
               this.changed.set(writePos, newSize);
            }

            this.changed.clear(newSize, oldSize);

            for (int scanPos = writePos; scanPos < oldSize && writePos < newSize; writePos++) {
               scanPos = this.deleted.nextClearBit(scanPos);
               if (scanPos != writePos) {
                  InstanceHandleImpl<I> handle = this.handles.get(scanPos);
                  I instance = this.instances.get(scanPos);
                  this.handles.set(writePos, handle);
                  this.instances.set(writePos, instance);
                  handle.index = writePos;
               }

               scanPos++;
            }

            this.deleted.clear();
            this.instances.subList(newSize, oldSize).clear();
            this.handles.subList(newSize, oldSize).clear();
         }
      }
   }

   @Override
   public void delete() {
      if (this.vbo != null) {
         this.vbo.delete();
         this.vbo = null;

         for (InstancedDraw instancedDraw : this.draws) {
            instancedDraw.delete();
         }
      }
   }

   public void addDrawCall(InstancedDraw instancedDraw) {
      this.draws.add(instancedDraw);
   }

   public void bind(TextureBuffer buffer) {
      if (this.vbo != null) {
         buffer.bind(this.vbo.handle());
      }
   }
}
