package dev.engine_room.flywheel.backend.engine.instancing;

import com.mojang.datafixers.util.Pair;
import dev.engine_room.flywheel.api.backend.Engine;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.compile.ContextShader;
import dev.engine_room.flywheel.backend.compile.InstancingPrograms;
import dev.engine_room.flywheel.backend.compile.PipelineCompiler;
import dev.engine_room.flywheel.backend.engine.AbstractInstancer;
import dev.engine_room.flywheel.backend.engine.CommonCrumbling;
import dev.engine_room.flywheel.backend.engine.DrawManager;
import dev.engine_room.flywheel.backend.engine.GroupKey;
import dev.engine_room.flywheel.backend.engine.InstanceHandleImpl;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import dev.engine_room.flywheel.backend.engine.LightStorage;
import dev.engine_room.flywheel.backend.engine.MaterialEncoder;
import dev.engine_room.flywheel.backend.engine.MaterialRenderState;
import dev.engine_room.flywheel.backend.engine.MeshPool;
import dev.engine_room.flywheel.backend.engine.TextureBinder;
import dev.engine_room.flywheel.backend.engine.embed.Environment;
import dev.engine_room.flywheel.backend.engine.embed.EnvironmentStorage;
import dev.engine_room.flywheel.backend.engine.indirect.OitFramebuffer;
import dev.engine_room.flywheel.backend.engine.uniform.Uniforms;
import dev.engine_room.flywheel.backend.gl.TextureBuffer;
import dev.engine_room.flywheel.backend.gl.array.GlVertexArray;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;

public class InstancedDrawManager extends DrawManager<InstancedInstancer<?>> {
   private static final Comparator<InstancedDraw> DRAW_COMPARATOR = Comparator.comparingInt(InstancedDraw::bias)
      .thenComparingInt(InstancedDraw::indexOfMeshInModel)
      .thenComparing(InstancedDraw::material, MaterialRenderState.COMPARATOR);
   private final List<InstancedDraw> allDraws = new ArrayList<>();
   private boolean needSort = false;
   private final List<InstancedDraw> draws = new ArrayList<>();
   private final List<InstancedDraw> oitDraws = new ArrayList<>();
   private final InstancingPrograms programs;
   private final MeshPool meshPool;
   private final GlVertexArray vao;
   private final TextureBuffer instanceTexture;
   private final InstancedLight light;
   private final OitFramebuffer oitFramebuffer;

   public InstancedDrawManager(InstancingPrograms programs) {
      programs.acquire();
      this.programs = programs;
      this.meshPool = new MeshPool();
      this.vao = GlVertexArray.create();
      this.instanceTexture = new TextureBuffer();
      this.light = new InstancedLight();
      this.meshPool.bind(this.vao);
      this.oitFramebuffer = new OitFramebuffer(programs.oitPrograms());
   }

   @Override
   public void render(LightStorage lightStorage, EnvironmentStorage environmentStorage) {
      super.render(lightStorage, environmentStorage);
      this.instancers.values().removeIf(instancer -> {
         if (instancer.instanceCount() == 0) {
            instancer.delete();
            return true;
         } else {
            instancer.updateBuffer();
            return false;
         }
      });
      this.needSort = this.needSort | this.allDraws.removeIf(InstancedDraw::deleted);
      if (this.needSort) {
         this.allDraws.sort(DRAW_COMPARATOR);
         this.draws.clear();
         this.oitDraws.clear();

         for (InstancedDraw draw : this.allDraws) {
            if (draw.material().transparency() == Transparency.ORDER_INDEPENDENT) {
               this.oitDraws.add(draw);
            } else {
               this.draws.add(draw);
            }
         }

         this.needSort = false;
      }

      this.meshPool.flush();
      this.light.flush(lightStorage);
      if (!this.allDraws.isEmpty()) {
         Uniforms.bindAll();
         this.vao.bindForDraw();
         TextureBinder.bindLightAndOverlay();
         this.light.bind();
         this.submitDraws();
         if (!this.oitDraws.isEmpty()) {
            this.oitFramebuffer.prepare();
            this.oitFramebuffer.depthRange();
            this.submitOitDraws(PipelineCompiler.OitMode.DEPTH_RANGE);
            this.oitFramebuffer.renderTransmittance();
            this.submitOitDraws(PipelineCompiler.OitMode.GENERATE_COEFFICIENTS);
            this.oitFramebuffer.renderDepthFromTransmittance();
            this.vao.bindForDraw();
            this.oitFramebuffer.accumulate();
            this.submitOitDraws(PipelineCompiler.OitMode.EVALUATE);
            this.oitFramebuffer.composite();
         }

         MaterialRenderState.reset();
         TextureBinder.resetLightAndOverlay();
      }
   }

   private void submitDraws() {
      for (InstancedDraw drawCall : this.draws) {
         Material material = drawCall.material();
         GroupKey<?> groupKey = drawCall.groupKey;
         Environment environment = groupKey.environment();
         GlProgram program = this.programs.get(groupKey.instanceType(), environment.contextShader(), material, PipelineCompiler.OitMode.OFF);
         program.bind();
         environment.setupDraw(program);
         uploadMaterialUniform(program, material);
         program.setUInt("_flw_baseVertex", drawCall.mesh().baseVertex());
         MaterialRenderState.setup(material);
         Samplers.INSTANCE_BUFFER.makeActive();
         drawCall.render(this.instanceTexture);
      }
   }

   private void submitOitDraws(PipelineCompiler.OitMode mode) {
      for (InstancedDraw drawCall : this.oitDraws) {
         Material material = drawCall.material();
         GroupKey<?> groupKey = drawCall.groupKey;
         Environment environment = groupKey.environment();
         GlProgram program = this.programs.get(groupKey.instanceType(), environment.contextShader(), material, mode);
         program.bind();
         environment.setupDraw(program);
         uploadMaterialUniform(program, material);
         program.setUInt("_flw_baseVertex", drawCall.mesh().baseVertex());
         MaterialRenderState.setupOit(material);
         Samplers.INSTANCE_BUFFER.makeActive();
         drawCall.render(this.instanceTexture);
      }
   }

   @Override
   public void delete() {
      this.instancers.values().forEach(InstancedInstancer::delete);
      this.allDraws.forEach(InstancedDraw::delete);
      this.allDraws.clear();
      this.draws.clear();
      this.oitDraws.clear();
      this.meshPool.delete();
      this.instanceTexture.delete();
      this.programs.release();
      this.vao.delete();
      this.light.delete();
      this.oitFramebuffer.delete();
      super.delete();
   }

   protected <I extends Instance> InstancedInstancer<I> create(InstancerKey<I> key) {
      return new InstancedInstancer<>(key, new AbstractInstancer.Recreate<>(key, this));
   }

   protected <I extends Instance> void initialize(InstancerKey<I> key, InstancedInstancer<?> instancer) {
      instancer.init();
      List<Model.ConfiguredMesh> meshes = key.model().meshes();

      for (int i = 0; i < meshes.size(); i++) {
         Model.ConfiguredMesh entry = meshes.get(i);
         MeshPool.PooledMesh mesh = this.meshPool.alloc(entry.mesh());
         GroupKey<?> groupKey = new GroupKey<>(key.type(), key.environment());
         InstancedDraw instancedDraw = new InstancedDraw(instancer, mesh, groupKey, entry.material(), key.bias(), i);
         this.allDraws.add(instancedDraw);
         this.needSort = true;
         instancer.addDrawCall(instancedDraw);
      }
   }

   @Override
   public void renderCrumbling(List<Engine.CrumblingBlock> crumblingBlocks) {
      Map<GroupKey<?>, ? extends Int2ObjectMap<? extends List<? extends Pair<? extends InstancedInstancer<?>, InstanceHandleImpl<?>>>>> byType = doCrumblingSort(
         crumblingBlocks, handle -> handle instanceof InstancedInstancer ? (InstancedInstancer)handle : null
      );
      if (!byType.isEmpty()) {
         SimpleMaterial.Builder crumblingMaterial = SimpleMaterial.builder();
         Uniforms.bindAll();
         this.vao.bindForDraw();
         TextureBinder.bindLightAndOverlay();

         for (Entry<GroupKey<?>, ? extends Int2ObjectMap<? extends List<? extends Pair<? extends InstancedInstancer<?>, InstanceHandleImpl<?>>>>> groupEntry : byType.entrySet()) {
            Int2ObjectMap<? extends List<? extends Pair<? extends InstancedInstancer<?>, InstanceHandleImpl<?>>>> byProgress = (Int2ObjectMap<? extends List<? extends Pair<? extends InstancedInstancer<?>, InstanceHandleImpl<?>>>>)groupEntry.getValue();
            GroupKey<?> shader = groupEntry.getKey();
            ObjectIterator var8 = byProgress.int2ObjectEntrySet().iterator();

            while (var8.hasNext()) {
               it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<? extends List<? extends Pair<? extends InstancedInstancer<?>, InstanceHandleImpl<?>>>> progressEntry = (it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry<? extends List<? extends Pair<? extends InstancedInstancer<?>, InstanceHandleImpl<?>>>>)var8.next();
               Samplers.CRUMBLING.makeActive();
               TextureBinder.bind((ResourceLocation)ModelBakery.BREAKING_LOCATIONS.get(progressEntry.getIntKey()));

               for (Pair<? extends InstancedInstancer<?>, InstanceHandleImpl<?>> instanceHandlePair : (List)progressEntry.getValue()) {
                  InstancedInstancer<?> instancer = (InstancedInstancer<?>)instanceHandlePair.getFirst();
                  int index = ((InstanceHandleImpl)instanceHandlePair.getSecond()).index;

                  for (InstancedDraw draw : instancer.draws()) {
                     CommonCrumbling.applyCrumblingProperties(crumblingMaterial, draw.material());
                     GlProgram program = this.programs.get(shader.instanceType(), ContextShader.CRUMBLING, crumblingMaterial, PipelineCompiler.OitMode.OFF);
                     program.bind();
                     program.setInt("_flw_baseInstance", index);
                     uploadMaterialUniform(program, crumblingMaterial);
                     MaterialRenderState.setup(crumblingMaterial);
                     Samplers.INSTANCE_BUFFER.makeActive();
                     draw.renderOne(this.instanceTexture);
                  }
               }
            }
         }

         MaterialRenderState.reset();
         TextureBinder.resetLightAndOverlay();
      }
   }

   @Override
   public void triggerFallback() {
      InstancingPrograms.kill();
      Minecraft.getInstance().levelRenderer.allChanged();
   }

   @Override
   public MeshPool meshPool() {
      return this.meshPool;
   }

   public static void uploadMaterialUniform(GlProgram program, Material material) {
      int packedFogAndCutout = MaterialEncoder.packUberShader(material);
      int packedMaterialProperties = MaterialEncoder.packProperties(material);
      program.setUVec2("_flw_packedMaterial", packedFogAndCutout, packedMaterialProperties);
   }
}
