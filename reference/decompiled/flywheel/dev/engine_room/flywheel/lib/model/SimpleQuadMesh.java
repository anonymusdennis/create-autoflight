package dev.engine_room.flywheel.lib.model;

import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.api.vertex.VertexList;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class SimpleQuadMesh implements QuadMesh {
   private final VertexList vertexList;
   private final Vector4f boundingSphere;
   @Nullable
   private final String descriptor;

   public SimpleQuadMesh(VertexList vertexList, @Nullable String descriptor) {
      this.vertexList = vertexList;
      this.boundingSphere = ModelUtil.computeBoundingSphere(vertexList);
      this.descriptor = descriptor;
   }

   public SimpleQuadMesh(VertexList vertexList) {
      this(vertexList, null);
   }

   @Override
   public int vertexCount() {
      return this.vertexList.vertexCount();
   }

   @Override
   public void write(MutableVertexList dst) {
      this.vertexList.writeAll(dst);
   }

   @Override
   public Vector4fc boundingSphere() {
      return this.boundingSphere;
   }

   @Override
   public String toString() {
      return "SimpleQuadMesh{vertexCount=" + this.vertexCount() + ",descriptor={" + this.descriptor + "}}";
   }
}
