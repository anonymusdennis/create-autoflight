package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.backend.InternalVertex;
import dev.engine_room.flywheel.backend.gl.GlPrimitive;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.backend.util.ReferenceCounted;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import dev.engine_room.flywheel.lib.vertex.VertexView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL32;

public class MeshPool {
   private final VertexView vertexView;
   private final Map<Mesh, MeshPool.PooledMesh> meshes = new HashMap<>();
   private final List<MeshPool.PooledMesh> meshList = new ArrayList<>();
   private final List<MeshPool.PooledMesh> recentlyAllocated = new ArrayList<>();
   private final GlBuffer vbo;
   private final IndexPool indexPool;
   private boolean dirty;
   private boolean anyToRemove;

   public MeshPool() {
      this.vertexView = InternalVertex.createVertexView();
      this.vbo = new GlBuffer(GlBufferUsage.DYNAMIC_DRAW);
      this.indexPool = new IndexPool();
   }

   public MeshPool.PooledMesh alloc(Mesh mesh) {
      return this.meshes.computeIfAbsent(mesh, this::_alloc);
   }

   private MeshPool.PooledMesh _alloc(Mesh m) {
      MeshPool.PooledMesh bufferedModel = new MeshPool.PooledMesh(m);
      this.meshList.add(bufferedModel);
      this.recentlyAllocated.add(bufferedModel);
      this.dirty = true;
      return bufferedModel;
   }

   @Nullable
   public MeshPool.PooledMesh get(Mesh mesh) {
      return this.meshes.get(mesh);
   }

   public void flush() {
      if (this.dirty) {
         if (this.anyToRemove) {
            this.anyToRemove = false;
            this.processDeletions();
         }

         if (!this.recentlyAllocated.isEmpty()) {
            for (MeshPool.PooledMesh mesh : this.recentlyAllocated) {
               this.indexPool.updateCount(mesh.mesh.indexSequence(), mesh.indexCount());
            }

            this.indexPool.flush();
            this.recentlyAllocated.clear();
         }

         this.uploadAll();
         this.dirty = false;
      }
   }

   private void processDeletions() {
      this.meshList.removeIf(pooledMesh -> {
         boolean deleted = pooledMesh.isDeleted();
         if (deleted) {
            this.meshes.remove(pooledMesh.mesh);
         }

         return deleted;
      });
   }

   private void uploadAll() {
      long neededSize = 0L;

      for (MeshPool.PooledMesh mesh : this.meshList) {
         neededSize += (long)mesh.byteSize();
      }

      MemoryBlock vertexBlock = MemoryBlock.malloc(neededSize);
      long vertexPtr = vertexBlock.ptr();
      int byteIndex = 0;
      int baseVertex = 0;

      for (MeshPool.PooledMesh mesh : this.meshList) {
         mesh.baseVertex = baseVertex;
         this.vertexView.ptr(vertexPtr + (long)byteIndex);
         this.vertexView.vertexCount(mesh.vertexCount());
         mesh.mesh.write(this.vertexView);
         byteIndex += mesh.byteSize();
         baseVertex += mesh.vertexCount();
      }

      this.vbo.upload(vertexBlock);
      vertexBlock.free();
   }

   public void bind(GlVertexArray vertexArray) {
      this.indexPool.bind(vertexArray);
      vertexArray.bindVertexBuffer(0, this.vbo.handle(), 0L, InternalVertex.STRIDE);
      vertexArray.bindAttributes(0, 0, InternalVertex.ATTRIBUTES);
   }

   public void delete() {
      this.vbo.delete();
      this.indexPool.delete();
      this.meshes.clear();
      this.meshList.clear();
   }

   public List<MeshPool.PooledMesh> pooledMeshes() {
      return this.meshList;
   }

   public class PooledMesh extends ReferenceCounted {
      public static final int INVALID_BASE_VERTEX = -1;
      private final Mesh mesh;
      private int baseVertex = -1;

      private PooledMesh(Mesh mesh) {
         this.mesh = mesh;
      }

      public int vertexCount() {
         return this.mesh.vertexCount();
      }

      public int byteSize() {
         return this.mesh.vertexCount() * InternalVertex.STRIDE;
      }

      public int indexCount() {
         return this.mesh.indexCount();
      }

      public int baseVertex() {
         return this.baseVertex;
      }

      public int firstIndex() {
         return MeshPool.this.indexPool.firstIndex(this.mesh.indexSequence());
      }

      public long firstIndexByteOffset() {
         return (long)this.firstIndex() * 4L;
      }

      public boolean isInvalid() {
         return this.mesh.vertexCount() == 0 || this.baseVertex == -1 || this.isDeleted();
      }

      public void draw(int instanceCount) {
         if (instanceCount > 1) {
            GL32.glDrawElementsInstancedBaseVertex(
               GlPrimitive.TRIANGLES.glEnum, this.mesh.indexCount(), 5125, this.firstIndexByteOffset(), instanceCount, this.baseVertex
            );
         } else {
            GL32.glDrawElementsBaseVertex(GlPrimitive.TRIANGLES.glEnum, this.mesh.indexCount(), 5125, this.firstIndexByteOffset(), this.baseVertex);
         }
      }

      @Override
      protected void _delete() {
         MeshPool.this.dirty = true;
         MeshPool.this.anyToRemove = true;
      }
   }
}
