package dev.engine_room.flywheel.lib.model;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.vertex.VertexList;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.material.StandardMaterialShaders;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.vertex.FullVertexView;
import dev.engine_room.flywheel.lib.vertex.VertexView;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.jetbrains.annotations.UnknownNullability;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

public final class LineModelBuilder {
   private static final Material MATERIAL = SimpleMaterial.builder()
      .shaders(StandardMaterialShaders.LINE)
      .backfaceCulling(false)
      .cardinalLightingMode(CardinalLightingMode.OFF)
      .build();
   @UnknownNullability
   private VertexView vertexView;
   @UnknownNullability
   private MemoryBlock data;
   private int vertexCount = 0;

   public LineModelBuilder() {
   }

   public LineModelBuilder(int initialSegmentCount) {
      this.ensureCapacity(initialSegmentCount);
   }

   public void ensureCapacity(int segmentCount) {
      if (segmentCount < 0) {
         throw new IllegalArgumentException("Segment count must be greater than or equal to 0");
      } else if (segmentCount != 0) {
         if (this.data == null) {
            this.vertexView = new FullVertexView();
            this.data = MemoryBlock.mallocTracked((long)(segmentCount * 4) * this.vertexView.stride());
            this.vertexView.ptr(this.data.ptr());
            this.vertexCount = 0;
         } else {
            long requiredCapacity = (long)(this.vertexCount + segmentCount * 4) * this.vertexView.stride();
            if (requiredCapacity > this.data.size()) {
               this.data = this.data.realloc(requiredCapacity);
               this.vertexView.ptr(this.data.ptr());
            }
         }
      }
   }

   public LineModelBuilder line(float x1, float y1, float z1, float x2, float y2, float z2) {
      this.ensureCapacity(1);
      float dx = x2 - x1;
      float dy = y2 - y1;
      float dz = z2 - z1;
      float length = (float)Math.sqrt((double)(dx * dx + dy * dy + dz * dz));
      float normalX = dx / length;
      float normalY = dy / length;
      float normalZ = dz / length;

      for (int i = 0; i < 2; i++) {
         this.vertexView.x(this.vertexCount + i, x1);
         this.vertexView.y(this.vertexCount + i, y1);
         this.vertexView.z(this.vertexCount + i, z1);
         this.vertexView.x(this.vertexCount + 2 + i, x2);
         this.vertexView.y(this.vertexCount + 2 + i, y2);
         this.vertexView.z(this.vertexCount + 2 + i, z2);
      }

      for (int i = 0; i < 4; i++) {
         this.vertexView.r(this.vertexCount + i, 1.0F);
         this.vertexView.g(this.vertexCount + i, 1.0F);
         this.vertexView.b(this.vertexCount + i, 1.0F);
         this.vertexView.a(this.vertexCount + i, 1.0F);
         this.vertexView.u(this.vertexCount + i, 0.0F);
         this.vertexView.v(this.vertexCount + i, 0.0F);
         this.vertexView.overlay(this.vertexCount + i, OverlayTexture.NO_OVERLAY);
         this.vertexView.light(this.vertexCount + i, 15728880);
         this.vertexView.normalX(this.vertexCount + i, normalX);
         this.vertexView.normalY(this.vertexCount + i, normalY);
         this.vertexView.normalZ(this.vertexCount + i, normalZ);
      }

      this.vertexCount += 4;
      return this;
   }

   public Model build() {
      if (this.vertexCount == 0) {
         return EmptyModel.INSTANCE;
      } else {
         long requiredCapacity = (long)this.vertexCount * this.vertexView.stride();
         if (this.data.size() > requiredCapacity) {
            this.data = this.data.realloc(requiredCapacity);
            this.vertexView.ptr(this.data.ptr());
         }

         this.vertexView.nativeMemoryOwner(this.data);
         this.vertexView.vertexCount(this.vertexCount);
         Vector4f boundingSphere = ModelUtil.computeBoundingSphere(this.vertexView);
         boundingSphere.w += 0.1F;
         LineModelBuilder.LineMesh mesh = new LineModelBuilder.LineMesh(this.vertexView, boundingSphere);
         SingleMeshModel model = new SingleMeshModel(mesh, MATERIAL);
         this.vertexView = null;
         this.data = null;
         this.vertexCount = 0;
         return model;
      }
   }

   private static class LineMesh implements Mesh {
      private static final IndexSequence INDEX_SEQUENCE = (ptr, count) -> {
         int numVertices = 2 * count / 3;
         int baseVertex = 0;

         while (baseVertex < numVertices) {
            MemoryUtil.memPutInt(ptr, baseVertex);
            MemoryUtil.memPutInt(ptr + 4L, baseVertex + 1);
            MemoryUtil.memPutInt(ptr + 8L, baseVertex + 2);
            MemoryUtil.memPutInt(ptr + 12L, baseVertex + 3);
            MemoryUtil.memPutInt(ptr + 16L, baseVertex + 2);
            MemoryUtil.memPutInt(ptr + 20L, baseVertex + 1);
            baseVertex += 4;
            ptr += 24L;
         }
      };
      private final VertexList vertexList;
      private final Vector4fc boundingSphere;

      public LineMesh(VertexList vertexList, Vector4fc boundingSphere) {
         this.vertexList = vertexList;
         this.boundingSphere = boundingSphere;
      }

      @Override
      public int vertexCount() {
         return this.vertexList.vertexCount();
      }

      @Override
      public void write(MutableVertexList vertexList) {
         this.vertexList.writeAll(vertexList);
      }

      @Override
      public IndexSequence indexSequence() {
         return INDEX_SEQUENCE;
      }

      @Override
      public int indexCount() {
         return this.vertexCount() / 2 * 3;
      }

      @Override
      public Vector4fc boundingSphere() {
         return this.boundingSphere;
      }
   }
}
