package dev.engine_room.flywheel.lib.model;

import dev.engine_room.flywheel.api.model.Model;
import java.util.Collections;
import java.util.List;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class EmptyModel implements Model {
   private static final Vector4fc BOUNDING_SPHERE = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
   public static final EmptyModel INSTANCE = new EmptyModel();

   @Override
   public List<Model.ConfiguredMesh> meshes() {
      return Collections.emptyList();
   }

   @Override
   public Vector4fc boundingSphere() {
      return BOUNDING_SPHERE;
   }
}
