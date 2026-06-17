package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import dev.engine_room.flywheel.backend.engine.MaterialRenderState;
import dev.engine_room.flywheel.backend.engine.MeshPool;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.lib.math.MoreMath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

public class IndirectCullingGroup<I extends Instance> {
   private static final Comparator<IndirectDraw> DRAW_COMPARATOR = Comparator.comparing(IndirectDraw::isEmbedded)
      .thenComparingInt(IndirectDraw::bias)
      .thenComparingInt(IndirectDraw::indexOfMeshInModel)
      .thenComparing(IndirectDraw::material, MaterialRenderState.COMPARATOR);
   private final InstanceType<I> instanceType;
   private final long instanceStride;
   private final IndirectBuffers buffers;
   private final List<IndirectInstancer<I>> instancers = new ArrayList<>();
   private final List<IndirectDraw> indirectDraws = new ArrayList<>();
   private final List<IndirectCullingGroup.MultiDraw> multiDraws = new ArrayList<>();
   private final List<IndirectCullingGroup.MultiDraw> oitDraws = new ArrayList<>();
   private final IndirectPrograms programs;
   private final GlProgram cullProgram;
   private boolean needsDrawBarrier;
   private boolean needsDrawSort;
   private int instanceCountThisFrame;

   IndirectCullingGroup(InstanceType<I> instanceType, IndirectPrograms programs) {
      this.instanceType = instanceType;
      this.instanceStride = (long)MoreMath.align4(instanceType.layout().byteSize());
      this.buffers = new IndirectBuffers(this.instanceStride);
      this.programs = programs;
      this.cullProgram = programs.getCullingProgram(instanceType);
   }

   public boolean flushInstancers() {
      this.instanceCountThisFrame = 0;
      int modelIndex = 0;
      Iterator<IndirectInstancer<I>> iterator = this.instancers.iterator();

      while (iterator.hasNext()) {
         IndirectInstancer<I> instancer = iterator.next();
         int instanceCount = instancer.instanceCount();
         if (instanceCount == 0) {
            iterator.remove();
            instancer.delete();
         } else {
            instancer.update(modelIndex, this.instanceCountThisFrame);
            this.instanceCountThisFrame += instanceCount;
            modelIndex++;
         }
      }

      if (this.indirectDraws.removeIf(IndirectDraw::deleted)) {
         this.needsDrawSort = true;
      }

      boolean out = this.indirectDraws.isEmpty();
      if (out) {
         this.delete();
      }

      return out;
   }

   public void upload(StagingBuffer stagingBuffer) {
      this.buffers.updateCounts(this.instanceCountThisFrame, this.instancers.size(), this.indirectDraws.size());
      this.uploadInstances(stagingBuffer);
      this.buffers.objectStorage.uploadDescriptors(stagingBuffer);
      this.uploadModels(stagingBuffer);
      if (this.needsDrawSort) {
         this.sortDraws();
         this.needsDrawSort = false;
      }

      this.uploadDraws(stagingBuffer);
      this.needsDrawBarrier = true;
   }

   public void dispatchCull() {
      Uniforms.bindAll();
      this.cullProgram.bind();
      this.buffers.bindForCull();
      GL43.glDispatchCompute(this.buffers.objectStorage.capacity(), 1, 1);
   }

   public void dispatchApply() {
      this.buffers.bindForApply();
      GL43.glDispatchCompute(GlCompat.getComputeGroupCount(this.indirectDraws.size()), 1, 1);
   }

   public boolean hasOitDraws() {
      return !this.oitDraws.isEmpty();
   }

   private void sortDraws() {
      this.multiDraws.clear();
      this.oitDraws.clear();
      this.indirectDraws.sort(DRAW_COMPARATOR);
      int start = 0;

      for (int i = 0; i < this.indirectDraws.size(); i++) {
         IndirectDraw draw1 = this.indirectDraws.get(i);
         if (i == this.indirectDraws.size() - 1 || this.incompatibleDraws(draw1, this.indirectDraws.get(i + 1))) {
            List<IndirectCullingGroup.MultiDraw> dst = draw1.material().transparency() == Transparency.ORDER_INDEPENDENT ? this.oitDraws : this.multiDraws;
            dst.add(new IndirectCullingGroup.MultiDraw(draw1.material(), draw1.isEmbedded(), start, i + 1));
            start = i + 1;
         }
      }
   }

   private boolean incompatibleDraws(IndirectDraw draw1, IndirectDraw draw2) {
      return draw1.isEmbedded() != draw2.isEmbedded() ? true : !MaterialRenderState.materialEquals(draw1.material(), draw2.material());
   }

   public void add(IndirectInstancer<I> instancer, InstancerKey<I> key, MeshPool meshPool) {
      instancer.mapping = this.buffers.objectStorage.createMapping();
      instancer.update(this.instancers.size(), -1);
      this.instancers.add(instancer);
      List<Model.ConfiguredMesh> meshes = key.model().meshes();

      for (int i = 0; i < meshes.size(); i++) {
         Model.ConfiguredMesh entry = meshes.get(i);
         MeshPool.PooledMesh mesh = meshPool.alloc(entry.mesh());
         IndirectDraw draw = new IndirectDraw(instancer, entry.material(), mesh, key.bias(), i);
         this.indirectDraws.add(draw);
         instancer.addDraw(draw);
      }

      this.needsDrawSort = true;
   }

   public void submitSolid() {
      if (!this.multiDraws.isEmpty()) {
         this.buffers.bindForDraw();
         this.drawBarrier();
         GlProgram lastProgram = null;

         for (IndirectCullingGroup.MultiDraw multiDraw : this.multiDraws) {
            GlProgram drawProgram = this.programs
               .getIndirectProgram(
                  this.instanceType, multiDraw.embedded ? ContextShader.EMBEDDED : ContextShader.DEFAULT, multiDraw.material, PipelineCompiler.OitMode.OFF
               );
            if (drawProgram != lastProgram) {
               lastProgram = drawProgram;
               drawProgram.bind();
            }

            MaterialRenderState.setup(multiDraw.material);
            multiDraw.submit(drawProgram);
         }
      }
   }

   public void submitTransparent(PipelineCompiler.OitMode oit) {
      if (!this.oitDraws.isEmpty()) {
         this.buffers.bindForDraw();
         this.drawBarrier();
         GlProgram lastProgram = null;

         for (IndirectCullingGroup.MultiDraw multiDraw : this.oitDraws) {
            GlProgram drawProgram = this.programs
               .getIndirectProgram(this.instanceType, multiDraw.embedded ? ContextShader.EMBEDDED : ContextShader.DEFAULT, multiDraw.material, oit);
            if (drawProgram != lastProgram) {
               lastProgram = drawProgram;
               drawProgram.bind();
               drawProgram.setFloat("_flw_blueNoiseFactor", 0.07F);
            }

            MaterialRenderState.setupOit(multiDraw.material);
            multiDraw.submit(drawProgram);
         }
      }
   }

   public void bindForCrumbling(Material material) {
      GlProgram program = this.programs.getIndirectProgram(this.instanceType, ContextShader.CRUMBLING, material, PipelineCompiler.OitMode.OFF);
      program.bind();
      this.buffers.bindForCrumbling();
      this.drawBarrier();
      program.setUInt("_flw_baseDraw", 0);
   }

   private void drawBarrier() {
      if (this.needsDrawBarrier) {
         GL42.glMemoryBarrier(64);
         this.needsDrawBarrier = false;
      }
   }

   private void uploadInstances(StagingBuffer stagingBuffer) {
      for (IndirectInstancer<I> instancer : this.instancers) {
         instancer.uploadInstances(stagingBuffer, this.buffers.objectStorage.objectBuffer.handle());
      }
   }

   private void uploadModels(StagingBuffer stagingBuffer) {
      long totalSize = (long)this.instancers.size() * 28L;
      int handle = this.buffers.model.handle();
      stagingBuffer.enqueueCopy(totalSize, handle, 0L, this::writeModels);
   }

   private void uploadDraws(StagingBuffer stagingBuffer) {
      long totalSize = (long)this.indirectDraws.size() * 36L;
      int handle = this.buffers.draw.handle();
      stagingBuffer.enqueueCopy(totalSize, handle, 0L, this::writeCommands);
   }

   private void writeModels(long writePtr) {
      for (IndirectInstancer<I> model : this.instancers) {
         model.writeModel(writePtr);
         writePtr += 28L;
      }
   }

   private void writeCommands(long writePtr) {
      for (IndirectDraw draw : this.indirectDraws) {
         draw.write(writePtr);
         writePtr += 36L;
      }
   }

   public void delete() {
      this.buffers.delete();
   }

   private static record MultiDraw(Material material, boolean embedded, int start, int end) {
      private void submit(GlProgram drawProgram) {
         GlCompat.safeMultiDrawElementsIndirect(drawProgram, 4, 5125, this.start, this.end, 36L);
      }
   }
}
