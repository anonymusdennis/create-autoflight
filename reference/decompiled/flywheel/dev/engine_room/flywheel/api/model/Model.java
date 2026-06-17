package dev.engine_room.flywheel.api.model;

import dev.engine_room.flywheel.api.material.Material;
import java.util.List;
import org.joml.Vector4fc;

public interface Model {
   List<Model.ConfiguredMesh> meshes();

   Vector4fc boundingSphere();

   public static record ConfiguredMesh(Material material, Mesh mesh) {
   }
}
