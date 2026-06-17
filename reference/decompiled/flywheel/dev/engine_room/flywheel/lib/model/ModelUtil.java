package dev.engine_room.flywheel.lib.model;

import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.VertexList;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.vertex.PosVertexView;
import java.util.Collection;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class ModelUtil {
   private static final float BOUNDING_SPHERE_EPSILON = 1.0E-4F;
   private static final RenderType[] CHUNK_LAYERS = new RenderType[]{
      RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent(), RenderType.tripwire()
   };
   private static final Material[] CHUNK_MATERIALS = new Material[20];

   private ModelUtil() {
   }

   @Nullable
   public static Material getMaterial(RenderType chunkRenderType, boolean shaded) {
      return getMaterial(chunkRenderType, shaded, true);
   }

   @Nullable
   public static Material getMaterial(RenderType chunkRenderType, boolean shaded, boolean ambientOcclusion) {
      for (int chunkLayerIdx = 0; chunkLayerIdx < CHUNK_LAYERS.length; chunkLayerIdx++) {
         if (chunkRenderType == CHUNK_LAYERS[chunkLayerIdx]) {
            int shadedIdx = shaded ? 1 : 0;
            int ambientOcclusionIdx = ambientOcclusion ? 1 : 0;
            int materialIdx = chunkLayerIdx * 4 + shadedIdx * 2 + ambientOcclusionIdx;
            return CHUNK_MATERIALS[materialIdx];
         }
      }

      return null;
   }

   @Nullable
   public static Material getItemMaterial(RenderType renderType) {
      Material chunkMaterial = getMaterial(renderType, true, false);
      if (chunkMaterial != null) {
         return chunkMaterial;
      } else if (renderType == Sheets.cutoutBlockSheet()) {
         return Materials.CUTOUT_BLOCK;
      } else if (renderType == Sheets.solidBlockSheet()) {
         return Materials.SOLID_BLOCK;
      } else if (renderType == Sheets.translucentCullBlockSheet() || renderType == Sheets.translucentItemSheet()) {
         return Materials.TRANSLUCENT_ENTITY;
      } else if (renderType == RenderType.glint() || renderType == RenderType.glintTranslucent()) {
         return Materials.GLINT;
      } else {
         return renderType != RenderType.entityGlint() && renderType != RenderType.entityGlintDirect() ? null : Materials.GLINT_ENTITY;
      }
   }

   public static int computeTotalVertexCount(Iterable<Mesh> meshes) {
      int vertexCount = 0;

      for (Mesh mesh : meshes) {
         vertexCount += mesh.vertexCount();
      }

      return vertexCount;
   }

   public static Vector4f computeBoundingSphere(Collection<Model.ConfiguredMesh> meshes) {
      return computeBoundingSphere(meshes.stream().map(Model.ConfiguredMesh::mesh).toList());
   }

   public static Vector4f computeBoundingSphere(Iterable<Mesh> meshes) {
      int vertexCount = computeTotalVertexCount(meshes);
      MemoryBlock block = MemoryBlock.malloc((long)vertexCount * 12L);
      PosVertexView vertexList = new PosVertexView();
      int baseVertex = 0;

      for (Mesh mesh : meshes) {
         vertexList.ptr(block.ptr() + (long)baseVertex * 12L);
         vertexList.vertexCount(mesh.vertexCount());
         mesh.write(vertexList);
         baseVertex += mesh.vertexCount();
      }

      vertexList.ptr(block.ptr());
      vertexList.vertexCount(vertexCount);
      Vector4f sphere = computeBoundingSphere(vertexList);
      block.free();
      return sphere;
   }

   public static Vector4f computeBoundingSphere(VertexList vertexList) {
      Vector3f center = computeCenterOfAABBContaining(vertexList);
      float radius = computeMaxDistanceTo(vertexList, center) + 1.0E-4F;
      return new Vector4f(center, radius);
   }

   private static float computeMaxDistanceTo(VertexList vertexList, Vector3f pos) {
      float farthestDistanceSquared = -1.0F;

      for (int i = 0; i < vertexList.vertexCount(); i++) {
         float distanceSquared = pos.distanceSquared(vertexList.x(i), vertexList.y(i), vertexList.z(i));
         if (distanceSquared > farthestDistanceSquared) {
            farthestDistanceSquared = distanceSquared;
         }
      }

      return (float)Math.sqrt((double)farthestDistanceSquared);
   }

   private static Vector3f computeCenterOfAABBContaining(VertexList vertexList) {
      Vector3f min = new Vector3f(Float.MAX_VALUE);
      Vector3f max = new Vector3f(Float.MIN_VALUE);

      for (int i = 0; i < vertexList.vertexCount(); i++) {
         float x = vertexList.x(i);
         float y = vertexList.y(i);
         float z = vertexList.z(i);
         min.x = Math.min(min.x, x);
         min.y = Math.min(min.y, y);
         min.z = Math.min(min.z, z);
         max.x = Math.max(max.x, x);
         max.y = Math.max(max.y, y);
         max.z = Math.max(max.z, z);
      }

      return min.add(max).mul(0.5F);
   }

   static {
      Material[] baseChunkMaterials = new Material[]{
         Materials.SOLID_BLOCK, Materials.CUTOUT_MIPPED_BLOCK, Materials.CUTOUT_BLOCK, Materials.TRANSLUCENT_BLOCK, Materials.TRIPWIRE_BLOCK
      };

      for (int chunkLayerIdx = 0; chunkLayerIdx < CHUNK_LAYERS.length; chunkLayerIdx++) {
         int baseMaterialIdx = chunkLayerIdx * 4;
         Material baseChunkMaterial = baseChunkMaterials[chunkLayerIdx];
         CHUNK_MATERIALS[baseMaterialIdx] = SimpleMaterial.builderOf(baseChunkMaterial)
            .cardinalLightingMode(CardinalLightingMode.OFF)
            .ambientOcclusion(false)
            .build();
         CHUNK_MATERIALS[baseMaterialIdx + 1] = SimpleMaterial.builderOf(baseChunkMaterial).cardinalLightingMode(CardinalLightingMode.OFF).build();
         CHUNK_MATERIALS[baseMaterialIdx + 2] = SimpleMaterial.builderOf(baseChunkMaterial).ambientOcclusion(false).build();
         CHUNK_MATERIALS[baseMaterialIdx + 3] = baseChunkMaterial;
      }
   }
}
