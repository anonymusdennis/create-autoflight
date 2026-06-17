package dev.ryanhcode.sable.sublevel.render.fancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer;
import foundry.veil.api.client.render.vertex.VertexArray;
import java.nio.IntBuffer;
import java.util.BitSet;
import org.lwjgl.opengl.ARBCopyBuffer;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

public class BucketRenderBuffer implements NativeResource {
   public static final int QUAD_SIZE = 8;
   private static final int DEFAULT_MAX_QUADS = 1000;
   private final StagingBuffer stagingBuffer;
   private int buffer;
   private boolean dirty;
   private int size;
   private int maxSize;
   private BitSet closedBuckets;

   public BucketRenderBuffer(StagingBuffer stagingBuffer) {
      this.stagingBuffer = stagingBuffer;
      this.buffer = GlStateManager._glGenBuffers();
      RenderSystem.glBindBuffer(34962, this.buffer);
      GL15C.glBufferData(34962, 8000L, 35040);
      this.maxSize = 1000;
      this.closedBuckets = new BitSet(this.maxSize);
      this.dirty = true;
   }

   private void resize(int newSize) {
      int copyDest = GlStateManager._glGenBuffers();
      RenderSystem.glBindBuffer(36662, this.buffer);
      RenderSystem.glBindBuffer(36663, copyDest);
      GL15C.glBufferData(36663, (long)newSize * 8L, 35040);
      ARBCopyBuffer.glCopyBufferSubData(36662, 36663, 0L, 0L, (long)this.maxSize * 8L);
      RenderSystem.glDeleteBuffers(this.buffer);
      this.buffer = copyDest;
      this.maxSize = newSize;
      BitSet old = this.closedBuckets;
      this.closedBuckets = new BitSet(this.maxSize);
      this.closedBuckets.or(old);
      this.dirty = true;
   }

   public void clear() {
      this.size = 0;
      if (this.maxSize > 1000) {
         RenderSystem.glBindBuffer(34962, this.buffer);
         GL15C.glBufferData(34962, 8000L, 35040);
         this.maxSize = 1000;
         this.closedBuckets = new BitSet(this.maxSize);
      } else {
         this.closedBuckets.clear();
      }
   }

   public BucketRenderBuffer.Slice allocate(int quadCount) {
      if (this.size + quadCount > this.maxSize) {
         this.resize((int)((double)(this.size + quadCount) * 1.5));
      }

      int fromIndex = this.closedBuckets.nextClearBit(0);

      int toIndex;
      while (true) {
         toIndex = this.closedBuckets.nextSetBit(fromIndex);
         if (toIndex == -1) {
            toIndex = fromIndex + quadCount - 1;
            break;
         }

         if (toIndex - fromIndex >= quadCount) {
            break;
         }

         fromIndex = this.closedBuckets.nextClearBit(toIndex);
      }

      if (toIndex >= this.maxSize) {
         this.resize((int)((double)toIndex * 1.5));
      }

      this.closedBuckets.set(fromIndex, toIndex + 1);
      this.size += quadCount;
      return new BucketRenderBuffer.Slice(this, fromIndex, quadCount);
   }

   public void free(BucketRenderBuffer.Slice slice) {
      if (!slice.closed) {
         this.closedBuckets.clear(slice.offset, slice.offset + slice.length);
         this.size = this.size - slice.length;
         slice.closed = true;
      }
   }

   public void bind(VertexArray vertexArray) {
      if (this.dirty) {
         this.dirty = false;
         vertexArray.editFormat().defineVertexBuffer(1, this.buffer, 0, 8, 1);
      }
   }

   public void free() {
      RenderSystem.glDeleteBuffers(this.buffer);
   }

   public static class Slice implements NativeResource {
      private final BucketRenderBuffer renderBuffer;
      private final int offset;
      private final int length;
      private boolean closed;

      private Slice(BucketRenderBuffer renderBuffer, int offset, int length) {
         this.renderBuffer = renderBuffer;
         this.offset = offset;
         this.length = length;
      }

      public long write() {
         return this.renderBuffer.stagingBuffer.reserve((long)this.length * 8L);
      }

      public IntBuffer writeInt() {
         long pointer = this.write();
         return (pointer & 3L) == 0L ? MemoryUtil.memIntBuffer(pointer, this.length * 8 / 4) : MemoryUtil.memByteBuffer(pointer, this.length * 8).asIntBuffer();
      }

      public void flush() {
         this.renderBuffer.stagingBuffer.copy(this.renderBuffer.buffer, (long)this.offset * 8L);
      }

      public int offset() {
         return this.offset;
      }

      public int length() {
         return this.length;
      }

      public void free() {
         this.renderBuffer.free(this);
      }
   }
}
