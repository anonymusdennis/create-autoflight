package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.backend.util.MemoryBuffer;
import org.lwjgl.system.MemoryUtil;

public class ScatterList {
   public static final long STRIDE = 8L;
   public final long maxBytesPerScatter;
   private final MemoryBuffer block = new MemoryBuffer(8L);
   private int length;
   private long usedBytes;

   public ScatterList() {
      this(256L);
   }

   private ScatterList(long maxBytesPerScatter) {
      if ((maxBytesPerScatter & 1020L) != maxBytesPerScatter) {
         throw new IllegalArgumentException("Max bytes per scatter must be a multiple of 4 and less than 1024");
      } else {
         this.maxBytesPerScatter = maxBytesPerScatter;
      }
   }

   public void pushTransfer(TransferList transfers, int transferIndex) {
      long size = transfers.size(transferIndex);
      long srcOffset = transfers.srcOffset(transferIndex);
      long dstOffset = transfers.dstOffset(transferIndex);
      long offset = 0L;
      long remaining = size;

      while (offset < size) {
         long copySize = Math.min(remaining, this.maxBytesPerScatter);
         this.push(copySize, srcOffset + offset, dstOffset + offset);
         offset += copySize;
         remaining -= copySize;
      }
   }

   public void push(long sizeBytes, long srcOffsetBytes, long dstOffsetBytes) {
      this.block.reallocIfNeeded(this.length);
      long ptr = this.block.ptrForIndex(this.length);
      MemoryUtil.memPutInt(ptr, packSizeAndSrcOffset(sizeBytes, srcOffsetBytes));
      MemoryUtil.memPutInt(ptr + 4L, (int)(dstOffsetBytes >> 2));
      this.length++;
      this.usedBytes += 8L;
   }

   public int copyCount() {
      return this.length;
   }

   public long usedBytes() {
      return this.usedBytes;
   }

   public boolean isEmpty() {
      return this.length == 0;
   }

   public void reset() {
      this.length = 0;
      this.usedBytes = 0L;
   }

   public long ptr() {
      return this.block.ptr();
   }

   public void delete() {
      this.block.delete();
   }

   private static int packSizeAndSrcOffset(long sizeBytes, long srcOffsetBytes) {
      int out = (int)(srcOffsetBytes >>> 2) & 16777215;
      return out | (int)(sizeBytes << 22) & 0xFF000000;
   }
}
