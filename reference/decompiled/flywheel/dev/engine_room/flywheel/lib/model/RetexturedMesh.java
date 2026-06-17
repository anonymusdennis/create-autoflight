package dev.engine_room.flywheel.lib.model;

import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.vertex.VertexTransformations;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector4fc;

public record RetexturedMesh(Mesh mesh, TextureAtlasSprite sprite) implements Mesh {
   @Override
   public int vertexCount() {
      return this.mesh.vertexCount();
   }

   @Override
   public void write(MutableVertexList vertexList) {
      this.mesh.write(vertexList);
      VertexTransformations.retexture(vertexList, this.sprite);
   }

   @Override
   public IndexSequence indexSequence() {
      return this.mesh.indexSequence();
   }

   @Override
   public int indexCount() {
      return this.mesh.indexCount();
   }

   @Override
   public Vector4fc boundingSphere() {
      return this.mesh.boundingSphere();
   }
}
