package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap.Entry;

public class IndexPool {
   private final GlBuffer ebo = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
   private final Reference2IntMap<IndexSequence> indexCounts = new Reference2IntOpenHashMap();
   private final Reference2IntMap<IndexSequence> firstIndices = new Reference2IntOpenHashMap();
   private boolean dirty;

   public IndexPool() {
      this.indexCounts.defaultReturnValue(0);
   }

   public int firstIndex(IndexSequence sequence) {
      return this.firstIndices.getInt(sequence);
   }

   public void reset() {
      this.indexCounts.clear();
      this.firstIndices.clear();
      this.dirty = true;
   }

   public void updateCount(IndexSequence sequence, int indexCount) {
      int oldCount = this.indexCounts.getInt(sequence);
      int newCount = Math.max(oldCount, indexCount);
      if (newCount > oldCount) {
         this.indexCounts.put(sequence, newCount);
         this.dirty = true;
      }
   }

   public void flush() {
      if (this.dirty) {
         this.firstIndices.clear();
         this.dirty = false;
         long totalIndexCount = 0L;
         IntIterator indexBlock = this.indexCounts.values().iterator();

         while (indexBlock.hasNext()) {
            int count = (Integer)indexBlock.next();
            totalIndexCount += (long)count;
         }

         MemoryBlock indexBlockx = MemoryBlock.malloc(totalIndexCount * 4L);
         long indexPtr = indexBlockx.ptr();
         int firstIndex = 0;
         ObjectIterator var7 = this.indexCounts.reference2IntEntrySet().iterator();

         while (var7.hasNext()) {
            Entry<IndexSequence> entries = (Entry<IndexSequence>)var7.next();
            IndexSequence indexSequence = (IndexSequence)entries.getKey();
            int indexCount = entries.getIntValue();
            this.firstIndices.put(indexSequence, firstIndex);
            indexSequence.fill(indexPtr + (long)firstIndex * 4L, indexCount);
            firstIndex += indexCount;
         }

         this.ebo.upload(indexBlockx);
         indexBlockx.free();
      }
   }

   public void bind(GlVertexArray vertexArray) {
      vertexArray.setElementBuffer(this.ebo.handle());
   }

   public void delete() {
      this.ebo.delete();
   }
}
