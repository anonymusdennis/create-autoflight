package dev.engine_room.flywheel.lib.vertex;

import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public interface VertexView extends MutableVertexList {
   long ptr();

   void ptr(long var1);

   void vertexCount(int var1);

   long stride();

   @Nullable
   Object nativeMemoryOwner();

   void nativeMemoryOwner(@Nullable Object var1);

   default void load(MemoryBlock data) {
      long bytes = data.size();
      long stride = this.stride();
      if (bytes % stride != 0L) {
         throw new IllegalArgumentException("MemoryBlock contains non-whole amount of vertices!");
      } else {
         int vertexCount = (int)(bytes / stride);
         this.ptr(data.ptr());
         this.vertexCount(vertexCount);
         this.nativeMemoryOwner(data);
      }
   }

   @Override
   default void write(MutableVertexList dst, int srcIndex, int dstIndex) {
      if (dst.getClass() == this.getClass()) {
         long stride = this.stride();
         long dstPtr = ((VertexView)dst).ptr();
         MemoryUtil.memCopy(this.ptr() + (long)srcIndex * stride, dstPtr + (long)dstIndex * stride, stride);
      } else {
         MutableVertexList.super.write(dst, srcIndex, dstIndex);
      }
   }

   @Override
   default void write(MutableVertexList dst, int srcStartIndex, int dstStartIndex, int vertexCount) {
      if (dst.getClass() == this.getClass()) {
         long stride = this.stride();
         long dstPtr = ((VertexView)dst).ptr();
         MemoryUtil.memCopy(this.ptr() + (long)srcStartIndex * stride, dstPtr + (long)dstStartIndex * stride, (long)vertexCount * stride);
      } else {
         MutableVertexList.super.write(dst, srcStartIndex, dstStartIndex, vertexCount);
      }
   }

   @Override
   default void writeAll(MutableVertexList dst) {
      if (dst.getClass() == this.getClass()) {
         long stride = this.stride();
         long dstPtr = ((VertexView)dst).ptr();
         MemoryUtil.memCopy(this.ptr(), dstPtr, (long)Math.min(this.vertexCount(), dst.vertexCount()) * stride);
      } else {
         MutableVertexList.super.writeAll(dst);
      }
   }
}
