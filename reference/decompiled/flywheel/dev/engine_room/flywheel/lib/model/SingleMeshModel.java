package dev.engine_room.flywheel.lib.model;

import com.google.common.collect.ImmutableList;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import java.util.List;
import org.joml.Vector4fc;

public class SingleMeshModel implements Model {
   private final Mesh mesh;
   private final ImmutableList<Model.ConfiguredMesh> meshList;

   public SingleMeshModel(Mesh mesh, Material material) {
      this.mesh = mesh;
      this.meshList = ImmutableList.of(new Model.ConfiguredMesh(material, mesh));
   }

   @Override
   public List<Model.ConfiguredMesh> meshes() {
      return this.meshList;
   }

   @Override
   public Vector4fc boundingSphere() {
      return this.mesh.boundingSphere();
   }
}
