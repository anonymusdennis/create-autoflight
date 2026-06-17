package dev.engine_room.flywheel.lib.model;

import dev.engine_room.flywheel.api.model.Model;
import java.util.List;
import org.joml.Vector4fc;

public class SimpleModel implements Model {
   private final List<Model.ConfiguredMesh> meshes;
   private final Vector4fc boundingSphere;

   public SimpleModel(List<Model.ConfiguredMesh> meshes) {
      this.meshes = meshes;
      this.boundingSphere = ModelUtil.computeBoundingSphere(meshes);
   }

   @Override
   public List<Model.ConfiguredMesh> meshes() {
      return this.meshes;
   }

   @Override
   public Vector4fc boundingSphere() {
      return this.boundingSphere;
   }
}
