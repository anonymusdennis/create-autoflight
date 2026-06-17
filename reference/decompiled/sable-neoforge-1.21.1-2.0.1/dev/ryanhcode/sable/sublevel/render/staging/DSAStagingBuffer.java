package dev.ryanhcode.sable.sublevel.render.staging;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;

@Internal
public class DSAStagingBuffer extends StagingBuffer {
   private final long size;
   private final int buffer;
   private final long pointer;
   private long writePointer;
   private long writeRegionSize;
   private final LongList flushRegions;
   private final List<DSAStagingBuffer.FencedArea> fences;

   DSAStagingBuffer(long size) {
      this.size = size;
      this.buffer = GlStateManager._glGenBuffers();
      GL45C.glNamedBufferStorage(this.buffer, size, 578);
      this.pointer = GL45C.nglMapNamedBufferRange(this.buffer, 0L, size, 90);
      this.flushRegions = new LongArrayList();
      this.fences = new ObjectArrayList();
      this.writePointer = 0L;
      this.writeRegionSize = size;
   }

   @Override
   public void updateFencedAreas() {
      if (!this.fences.isEmpty()) {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            IntBuffer size = stack.mallocInt(1);
            Iterator<DSAStagingBuffer.FencedArea> iterator = this.fences.iterator();

            while (iterator.hasNext()) {
               DSAStagingBuffer.FencedArea area = iterator.next();
               long fence = area.fence;
               int status = GL45C.glGetSynci(fence, 37140, size);
               if (size.get(0) != 1) {
                  throw new IllegalStateException("Expected 1 value from fence");
               }

               if (status == 37145) {
                  GL45C.glDeleteSync(fence);
                  iterator.remove();
               }
            }
         } catch (Throwable var9) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stack != null) {
            stack.close();
         }
      }
   }

   private long allocate(long size) {
      long pointer = this.pointer + this.writePointer;
      if (!this.flushRegions.isEmpty()) {
         long offset = this.flushRegions.getLong(this.flushRegions.size() - 2);
         long length = this.flushRegions.getLong(this.flushRegions.size() - 1);
         if (offset + length == this.writePointer) {
            this.flushRegions.set(this.flushRegions.size() - 1, length + size);
         } else {
            this.flushRegions.add(this.writePointer);
            this.flushRegions.add(size);
         }
      } else {
         this.flushRegions.add(this.writePointer);
         this.flushRegions.add(size);
      }

      this.writePointer += size;
      this.writeRegionSize -= size;
      return pointer;
   }

   @Override
   public long reserve(long size) {
      if (this.writePointer + size >= this.writeRegionSize) {
         this.updateFencedAreas();
         if (this.fences.isEmpty()) {
            this.writePointer = 0L;
            this.writeRegionSize = this.size;
            return this.allocate(size);
         }

         DSAStagingBuffer.FencedArea fence = this.fences.getLast();
         if (fence.offset + fence.length + size < this.size) {
            long end = fence.offset + fence.length;

            for (int i = 0; i < this.flushRegions.size(); i += 2) {
               long offset = this.flushRegions.getLong(i);
               long length = this.flushRegions.getLong(this.flushRegions.size() - 1);
               if (offset + length + size >= this.size) {
                  this.writePointer = 0L;
                  this.writeRegionSize = this.fences.getFirst().offset;
                  return this.allocate(size);
               }

               if (offset + length > end) {
                  end = offset + length;
               }
            }

            this.writePointer = end;
            this.writeRegionSize = this.size - this.writePointer;
            return this.allocate(size);
         }

         this.writePointer = 0L;
         this.writeRegionSize = this.fences.getFirst().offset;
      }

      return this.allocate(size);
   }

   @Override
   public void copy(int buffer, long writeOffset) {
      if (!this.flushRegions.isEmpty()) {
         long writeRegionOffset = 0L;
         long offset = this.flushRegions.getLong(0);
         long length = this.flushRegions.getLong(1);

         for (int i = 2; i < this.flushRegions.size(); i += 2) {
            long regionOffset = this.flushRegions.getLong(i);
            long regionLength = this.flushRegions.getLong(i + 1);
            if (offset + length == regionOffset) {
               length += regionLength;
            } else {
               GL45C.glFlushMappedNamedBufferRange(this.buffer, offset, length);
               GL45C.glCopyNamedBufferSubData(this.buffer, buffer, offset, writeRegionOffset + writeOffset, length);
               this.fences.add(new DSAStagingBuffer.FencedArea(GL45C.glFenceSync(37143, 0), offset, length));
               writeRegionOffset += length;
               offset = regionOffset;
               length = regionLength;
            }
         }

         GL45C.glFlushMappedNamedBufferRange(this.buffer, offset, length);
         GL45C.glCopyNamedBufferSubData(this.buffer, buffer, offset, writeRegionOffset + writeOffset, length);
         this.fences.add(new DSAStagingBuffer.FencedArea(GL45C.glFenceSync(37143, 0), offset, length));
         this.flushRegions.clear();
         Collections.sort(this.fences);
      }
   }

   @Override
   public long getSize() {
      return this.size;
   }

   @Override
   public long getUsedSize() {
      return this.writePointer;
   }

   public void free() {
      GL45C.glUnmapNamedBuffer(this.buffer);
      GL45C.glDeleteBuffers(this.buffer);
   }

   private static record FencedArea(long fence, long offset, long length) implements Comparable<DSAStagingBuffer.FencedArea> {
      public int compareTo(@NotNull DSAStagingBuffer.FencedArea o) {
         return Long.compare(this.offset, o.offset);
      }
   }
}
