package dev.engine_room.flywheel.lib.model.baked;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.model.SimpleQuadMesh;
import dev.engine_room.flywheel.lib.vertex.NoOverlayVertexView;
import dev.engine_room.flywheel.lib.vertex.VertexView;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

final class MeshHelper {
   private MeshHelper() {
   }

   public static SimpleQuadMesh blockVerticesToMesh(MeshData data, @Nullable String meshDescriptor) {
      DrawState drawState = data.drawState();
      int vertexCount = drawState.vertexCount();
      long srcStride = (long)drawState.format().getVertexSize();
      VertexView vertexView = new NoOverlayVertexView();
      long dstStride = vertexView.stride();
      ByteBuffer src = data.vertexBuffer();
      MemoryBlock dst = MemoryBlock.mallocTracked((long)vertexCount * dstStride);
      long srcPtr = MemoryUtil.memAddress(src);
      long dstPtr = dst.ptr();
      long bytesToCopy = Math.min(dstStride, 31L);

      for (int i = 0; i < vertexCount; i++) {
         MemoryUtil.memCopy(srcPtr + srcStride * (long)i, dstPtr + dstStride * (long)i, bytesToCopy);
      }

      vertexView.ptr(dstPtr);
      vertexView.vertexCount(vertexCount);
      vertexView.nativeMemoryOwner(dst);
      return new SimpleQuadMesh(vertexView, meshDescriptor);
   }
}
