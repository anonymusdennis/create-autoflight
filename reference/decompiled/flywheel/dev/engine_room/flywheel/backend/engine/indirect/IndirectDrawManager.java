package dev.engine_room.flywheel.backend.engine.indirect;

import com.mojang.datafixers.util.Pair;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import dev.engine_room.flywheel.backend.engine.AbstractInstancer;
import dev.engine_room.flywheel.backend.engine.CommonCrumbling;
import dev.engine_room.flywheel.backend.engine.DrawManager;
import dev.engine_room.flywheel.backend.engine.GroupKey;
import dev.engine_room.flywheel.backend.engine.InstanceHandleImpl;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.MaterialRenderState;
import dev.engine_room.flywheel.backend.engine.MeshPool;
import dev.engine_room.flywheel.backend.engine.TextureBinder;
import dev.engine_room.flywheel.backend.engine.embed.EnvironmentStorage;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.buffer.GlBuffer;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferType;
import dev.engine_room.flywheel.backend.gl.buffer.GlBufferUsage;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;

public class IndirectDrawManager extends DrawManager<IndirectInstancer<?>> {
   private final IndirectPrograms programs;
   private final StagingBuffer stagingBuffer;
   private final MeshPool meshPool;
   private final GlVertexArray vertexArray;
   private final Map<InstanceType<?>, IndirectCullingGroup<?>> cullingGroups = new HashMap<>();
   private final GlBuffer crumblingDrawBuffer = new GlBuffer(GlBufferUsage.STREAM_DRAW);
   private final LightBuffers lightBuffers;
   private final MatrixBuffer matrixBuffer;
   private final DepthPyramid depthPyramid;
   private final OitFramebuffer oitFramebuffer;

   public IndirectDrawManager(IndirectPrograms programs) {
      this.programs = programs;
      programs.acquire();
      this.stagingBuffer = new StagingBuffer(this.programs);
      this.meshPool = new MeshPool();
      this.vertexArray = GlVertexArray.create();
      this.meshPool.bind(this.vertexArray);
      this.lightBuffers = new LightBuffers();
      this.matrixBuffer = new MatrixBuffer();
      this.depthPyramid = new DepthPyramid(programs);
      this.oitFramebuffer = new OitFramebuffer(programs.oitPrograms());
   }

   protected <I extends Instance> IndirectInstancer<?> create(InstancerKey<I> key) {
      return new IndirectInstancer<>(key, new AbstractInstancer.Recreate<>(key, this));
   }

   protected <I extends Instance> void initialize(InstancerKey<I> key, IndirectInstancer<?> instancer) {
      IndirectCullingGroup<I> group = (IndirectCullingGroup<I>)this.cullingGroups
         .computeIfAbsent(key.type(), t -> new IndirectCullingGroup<>((InstanceType<?>)t, this.programs));
      group.add((IndirectInstancer<I>)instancer, key, this.meshPool);
   }

   @Override
   public void render(LightStorage lightStorage, EnvironmentStorage environmentStorage) {
      super.render(lightStorage, environmentStorage);
      this.cullingGroups.values().removeIf(IndirectCullingGroup::flushInstancers);
      this.instancers.values().removeIf(instancer -> instancer.instanceCount() == 0);
      this.meshPool.flush();
      this.stagingBuffer.reclaim();
      if (!this.cullingGroups.isEmpty()) {
         this.lightBuffers.flush(this.stagingBuffer, lightStorage);
         this.matrixBuffer.flush(this.stagingBuffer, environmentStorage);

         for (IndirectCullingGroup<?> group : this.cullingGroups.values()) {
            group.upload(this.stagingBuffer);
         }

         this.stagingBuffer.flush();
         this.depthPyramid.generate();
         GL42.glMemoryBarrier(8192);
         this.matrixBuffer.bind();
         this.depthPyramid.bindForCull();

         for (IndirectCullingGroup<?> group : this.cullingGroups.values()) {
            group.dispatchCull();
         }

         GL42.glMemoryBarrier(8192);
         this.programs.getApplyProgram().bind();

         for (IndirectCullingGroup<?> group : this.cullingGroups.values()) {
            group.dispatchApply();
         }

         GL42.glMemoryBarrier(8192);
         TextureBinder.bindLightAndOverlay();
         this.vertexArray.bindForDraw();
         this.lightBuffers.bind();
         this.matrixBuffer.bind();
         Uniforms.bindAll();

         for (IndirectCullingGroup<?> group : this.cullingGroups.values()) {
            group.submitSolid();
         }

         boolean useOit = false;

         for (IndirectCullingGroup<?> group : this.cullingGroups.values()) {
            if (group.hasOitDraws()) {
               useOit = true;
               break;
            }
         }

         if (useOit) {
            this.oitFramebuffer.prepare();
            this.oitFramebuffer.depthRange();

            for (IndirectCullingGroup<?> groupx : this.cullingGroups.values()) {
               groupx.submitTransparent(PipelineCompiler.OitMode.DEPTH_RANGE);
            }

            this.oitFramebuffer.renderTransmittance();

            for (IndirectCullingGroup<?> groupx : this.cullingGroups.values()) {
               groupx.submitTransparent(PipelineCompiler.OitMode.GENERATE_COEFFICIENTS);
            }

            this.oitFramebuffer.renderDepthFromTransmittance();
            this.vertexArray.bindForDraw();
            this.oitFramebuffer.accumulate();

            for (IndirectCullingGroup<?> groupx : this.cullingGroups.values()) {
               groupx.submitTransparent(PipelineCompiler.OitMode.EVALUATE);
            }

            this.oitFramebuffer.composite();
         }

         MaterialRenderState.reset();
         TextureBinder.resetLightAndOverlay();
      }
   }

   @Override
   public void delete() {
      super.delete();
      this.cullingGroups.values().forEach(IndirectCullingGroup::delete);
      this.cullingGroups.clear();
      this.stagingBuffer.delete();
      this.meshPool.delete();
      this.crumblingDrawBuffer.delete();
      this.programs.release();
      this.depthPyramid.delete();
      this.lightBuffers.delete();
      this.matrixBuffer.delete();
      this.oitFramebuffer.delete();
   }

   @Override
   public void renderCrumbling(List<Engine.CrumblingBlock> crumblingBlocks) {
      Map<GroupKey<?>, Int2ObjectMap<List<Pair<IndirectInstancer<?>, InstanceHandleImpl<?>>>>> byType = doCrumblingSort(
         crumblingBlocks, IndirectInstancer::fromState
      );
      if (!byType.isEmpty()) {
         TextureBinder.bindLightAndOverlay();
         this.vertexArray.bindForDraw();
         Uniforms.bindAll();
         SimpleMaterial.Builder crumblingMaterial = SimpleMaterial.builder();
         MemoryBlock block = MemoryBlock.malloc(36L);
         GlBufferType.DRAW_INDIRECT_BUFFER.bind(this.crumblingDrawBuffer.handle());
         GL30.glBindBufferRange(37074, 4, this.crumblingDrawBuffer.handle(), 0L, 36L);

         for (Entry<GroupKey<?>, Int2ObjectMap<List<Pair<IndirectInstancer<?>, InstanceHandleImpl<?>>>>> groupEntry : byType.entrySet()) {
            Int2ObjectMap<List<Pair<IndirectInstancer<?>, InstanceHandleImpl<?>>>> byProgress = groupEntry.getValue();
            GroupKey<?> groupKey = groupEntry.getKey();
            IndirectCullingGroup<?> cullingGroup = this.cullingGroups.get(groupKey.instanceType());
            if (cullingGroup != null) {
               ObjectIterator var10 = byProgress.int2ObjectEntrySet().iterator();

               while (var10.hasNext()) {
                  it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<List<Pair<IndirectInstancer<?>, InstanceHandleImpl<?>>>> progressEntry = (it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<List<Pair<IndirectInstancer<?>, InstanceHandleImpl<?>>>>)var10.next();
                  Samplers.CRUMBLING.makeActive();
                  TextureBinder.bind((ResourceLocation)ModelBakery.BREAKING_LOCATIONS.get(progressEntry.getIntKey()));

                  for (Pair<IndirectInstancer<?>, InstanceHandleImpl<?>> instanceHandlePair : (List)progressEntry.getValue()) {
                     IndirectInstancer<?> instancer = (IndirectInstancer<?>)instanceHandlePair.getFirst();
                     int instanceIndex = ((InstanceHandleImpl)instanceHandlePair.getSecond()).index;

                     for (IndirectDraw draw : instancer.draws()) {
                        CommonCrumbling.applyCrumblingProperties(crumblingMaterial, draw.material());
                        cullingGroup.bindForCrumbling(crumblingMaterial);
                        MaterialRenderState.setup(crumblingMaterial);
                        draw.writeWithOverrides(block.ptr(), instanceIndex, crumblingMaterial);
                        this.crumblingDrawBuffer.upload(block);
                        GL40.glDrawElementsIndirect(4, 5125, 0L);
                     }
                  }
               }
            }
         }

         MaterialRenderState.reset();
         TextureBinder.resetLightAndOverlay();
         block.free();
      }
   }

   @Override
   public void triggerFallback() {
      IndirectPrograms.kill();
      Minecraft.getInstance().levelRenderer.allChanged();
   }

   @Override
   public MeshPool meshPool() {
      return this.meshPool;
   }
}
