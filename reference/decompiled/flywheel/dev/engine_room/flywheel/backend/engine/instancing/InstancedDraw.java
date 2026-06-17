package dev.engine_room.flywheel.backend.engine.instancing;

import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.backend.engine.GroupKey;
import dev.engine_room.flywheel.backend.engine.MeshPool;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;

public class InstancedDraw {
   public final GroupKey<?> groupKey;
   private final InstancedInstancer<?> instancer;
   private final MeshPool.PooledMesh mesh;
   private final Material material;
   private final int bias;
   private final int indexOfMeshInModel;
   private boolean deleted;

   public InstancedDraw(InstancedInstancer<?> instancer, MeshPool.PooledMesh mesh, GroupKey<?> groupKey, Material material, int bias, int indexOfMeshInModel) {
      this.instancer = instancer;
      this.mesh = mesh;
      this.groupKey = groupKey;
      this.material = material;
      this.bias = bias;
      this.indexOfMeshInModel = indexOfMeshInModel;
      mesh.acquire();
   }

   public int bias() {
      return this.bias;
   }

   public int indexOfMeshInModel() {
      return this.indexOfMeshInModel;
   }

   public Material material() {
      return this.material;
   }

   public boolean deleted() {
      return this.deleted;
   }

   public MeshPool.PooledMesh mesh() {
      return this.mesh;
   }

   public void render(TextureBuffer buffer) {
      if (!this.mesh.isInvalid()) {
         this.instancer.bind(buffer);
         this.mesh.draw(this.instancer.instanceCount());
      }
   }

   public void renderOne(TextureBuffer buffer) {
      if (!this.mesh.isInvalid()) {
         this.instancer.bind(buffer);
         this.mesh.draw(1);
      }
   }

   public void delete() {
      if (!this.deleted) {
         this.mesh.release();
         this.deleted = true;
      }
   }
}
