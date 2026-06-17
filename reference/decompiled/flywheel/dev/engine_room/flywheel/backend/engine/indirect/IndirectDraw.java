package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.backend.engine.MaterialEncoder;
import dev.engine_room.flywheel.backend.engine.MeshPool;
import dev.engine_room.flywheel.backend.engine.embed.EmbeddedEnvironment;
import org.lwjgl.system.MemoryUtil;

public class IndirectDraw {
   private final IndirectInstancer<?> instancer;
   private final Material material;
   private final MeshPool.PooledMesh mesh;
   private final int bias;
   private final int indexOfMeshInModel;
   private final int packedFogAndCutout;
   private final int packedMaterialProperties;
   private boolean deleted;

   public IndirectDraw(IndirectInstancer<?> instancer, Material material, MeshPool.PooledMesh mesh, int bias, int indexOfMeshInModel) {
      this.instancer = instancer;
      this.material = material;
      this.mesh = mesh;
      this.bias = bias;
      this.indexOfMeshInModel = indexOfMeshInModel;
      mesh.acquire();
      this.packedFogAndCutout = MaterialEncoder.packUberShader(material);
      this.packedMaterialProperties = MaterialEncoder.packProperties(material);
   }

   public boolean deleted() {
      return this.deleted;
   }

   public Material material() {
      return this.material;
   }

   public boolean isEmbedded() {
      return this.instancer.environment instanceof EmbeddedEnvironment;
   }

   public MeshPool.PooledMesh mesh() {
      return this.mesh;
   }

   public int bias() {
      return this.bias;
   }

   public int indexOfMeshInModel() {
      return this.indexOfMeshInModel;
   }

   public void write(long ptr) {
      MemoryUtil.memPutInt(ptr, this.mesh.indexCount());
      MemoryUtil.memPutInt(ptr + 4L, 0);
      MemoryUtil.memPutInt(ptr + 8L, this.mesh.firstIndex());
      MemoryUtil.memPutInt(ptr + 12L, this.mesh.baseVertex());
      MemoryUtil.memPutInt(ptr + 16L, this.instancer.baseInstance());
      MemoryUtil.memPutInt(ptr + 20L, this.instancer.modelIndex());
      MemoryUtil.memPutInt(ptr + 24L, this.instancer.environment.matrixIndex());
      MemoryUtil.memPutInt(ptr + 28L, this.packedFogAndCutout);
      MemoryUtil.memPutInt(ptr + 32L, this.packedMaterialProperties);
   }

   public void writeWithOverrides(long ptr, int instanceIndex, Material materialOverride) {
      MemoryUtil.memPutInt(ptr, this.mesh.indexCount());
      MemoryUtil.memPutInt(ptr + 4L, 1);
      MemoryUtil.memPutInt(ptr + 8L, this.mesh.firstIndex());
      MemoryUtil.memPutInt(ptr + 12L, this.mesh.baseVertex());
      MemoryUtil.memPutInt(ptr + 16L, this.instancer.local2GlobalInstanceIndex(instanceIndex));
      MemoryUtil.memPutInt(ptr + 20L, this.instancer.modelIndex());
      MemoryUtil.memPutInt(ptr + 24L, this.instancer.environment.matrixIndex());
      MemoryUtil.memPutInt(ptr + 28L, MaterialEncoder.packUberShader(materialOverride));
      MemoryUtil.memPutInt(ptr + 32L, MaterialEncoder.packProperties(materialOverride));
   }

   public void delete() {
      if (!this.deleted) {
         this.mesh.release();
         this.deleted = true;
      }
   }
}
