package dev.engine_room.flywheel.lib.model.baked;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;

class ByteBufferBuilderStack {
   private static final int INITIAL_CAPACITY_BYTES = 8192;
   private int nextBufferBuilderIndex = 0;
   private final ReferenceArrayList<ByteBufferBuilder> bufferBuilders = new ReferenceArrayList();

   ByteBufferBuilder nextOrCreate() {
      ByteBufferBuilder bufferBuilder;
      if (this.nextBufferBuilderIndex < this.bufferBuilders.size()) {
         bufferBuilder = (ByteBufferBuilder)this.bufferBuilders.get(this.nextBufferBuilderIndex);
      } else {
         bufferBuilder = new ByteBufferBuilder(8192);
         this.bufferBuilders.add(bufferBuilder);
      }

      this.nextBufferBuilderIndex++;
      return bufferBuilder;
   }

   public void reset() {
      this.nextBufferBuilderIndex = 0;
   }
}
