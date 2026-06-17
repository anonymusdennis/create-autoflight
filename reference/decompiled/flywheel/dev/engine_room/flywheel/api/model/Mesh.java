package dev.engine_room.flywheel.api.model;

import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import org.joml.Vector4fc;

public interface Mesh {
   int vertexCount();

   void write(MutableVertexList var1);

   IndexSequence indexSequence();

   int indexCount();

   Vector4fc boundingSphere();
}
