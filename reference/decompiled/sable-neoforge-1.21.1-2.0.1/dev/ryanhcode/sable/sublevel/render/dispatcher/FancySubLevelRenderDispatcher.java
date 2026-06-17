package dev.ryanhcode.sable.sublevel.render.dispatcher;

import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.render.sky_light_shadow.SableSkyLightShadows;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelCommandBuilder;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelOcclusionData;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.fancy.FancySubLevelSectionCompiler;
import dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniform;
import foundry.veil.api.client.render.vertex.VertexArray;
import foundry.veil.api.client.render.vertex.VertexArray.DrawUsage;
import foundry.veil.api.client.render.vertex.VertexArray.IndexType;
import foundry.veil.api.client.render.vertex.VertexArrayBuilder.DataType;
import foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3ic;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

public class FancySubLevelRenderDispatcher implements SubLevelRenderDispatcher {
   private static final Matrix4f TRANSFORM = new Matrix4f();
   private static final int VERTEX_SIZE = 8;
   private final Map<String, CompletableFuture<ShaderProgram>> dynamicPrograms = new Object2ObjectArrayMap();
   private final StagingBuffer stagingBuffer = StagingBuffer.create();
   private final FancySubLevelSectionCompiler sectionCompiler = new FancySubLevelSectionCompiler(
      this.stagingBuffer, Minecraft.getInstance().getBlockRenderer(), Minecraft.getInstance().getBlockEntityRenderDispatcher()
   );
   private final FancySubLevelCommandBuilder commandBuilder = new FancySubLevelCommandBuilder(this.stagingBuffer);
   private final VertexArray vertexArray = VertexArray.create();

   public FancySubLevelRenderDispatcher() {
      int vbo = this.vertexArray.getOrCreateBuffer(0);
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer buffer = stack.malloc(192);
         buffer.put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)-1).put((byte)0).put((byte)0);
         buffer.put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)-1).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)-1).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)-1).put((byte)0).put((byte)0);
         buffer.put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)0);
         buffer.put((byte)0).put((byte)1).put((byte)1).put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)1).put((byte)1).put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)-1).put((byte)0);
         buffer.put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)-1).put((byte)0);
         buffer.put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)-1).put((byte)0);
         buffer.put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)-1).put((byte)0);
         buffer.put((byte)0).put((byte)1).put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)1).put((byte)0);
         buffer.put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)1).put((byte)0);
         buffer.put((byte)1).put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)1).put((byte)0);
         buffer.put((byte)1).put((byte)1).put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)1).put((byte)0);
         buffer.put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)-1).put((byte)0).put((byte)0).put((byte)0);
         buffer.put((byte)0).put((byte)0).put((byte)0).put((byte)0).put((byte)-1).put((byte)0).put((byte)0).put((byte)0);
         buffer.put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)-1).put((byte)0).put((byte)0).put((byte)0);
         buffer.put((byte)0).put((byte)1).put((byte)1).put((byte)0).put((byte)-1).put((byte)0).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)1).put((byte)1).put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)0).put((byte)1).put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)0).put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)0);
         buffer.put((byte)1).put((byte)1).put((byte)0).put((byte)0).put((byte)1).put((byte)0).put((byte)0).put((byte)0);
         buffer.flip();
         ByteBuffer indices = stack.bytes(new byte[]{0, 1, 2, 2, 3, 0});
         VertexArray.upload(vbo, buffer, DrawUsage.STATIC);
         this.vertexArray.uploadIndexBuffer(indices, IndexType.BYTE);
      } catch (Throwable var6) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (stack != null) {
         stack.close();
      }

      this.vertexArray
         .editFormat()
         .defineVertexBuffer(0, vbo, 0, 8, 0)
         .setVertexAttribute(0, 0, 3, DataType.BYTE, false, 0)
         .setVertexAttribute(1, 0, 3, DataType.BYTE, false, 4)
         .setVertexIAttribute(2, 1, 2, DataType.UNSIGNED_INT, 0);
   }

   public void onResourceManagerReload(ResourceManager resourceManager) {
      this.freePrograms();
   }

   @Override
   public SubLevelRenderData resize(ClientSubLevel subLevel, SubLevelRenderData renderData) {
      ((FancySubLevelRenderData)renderData).resize();
      return renderData;
   }

   @Override
   public SubLevelRenderData createRenderData(ClientSubLevel subLevel) {
      return new FancySubLevelRenderData(subLevel, this.sectionCompiler);
   }

   @Nullable
   private ShaderProgram getDynamicProgram(ShaderInstance vanillaProgram) {
      String name = VanillaShaderCompiler.getActiveDynamicBuffers(vanillaProgram) + "/" + vanillaProgram.getName();
      CompletableFuture<ShaderProgram> future = this.dynamicPrograms.get(name);
      if (future != null) {
         return future.getNow(null);
      } else {
         MemoryStack stack = MemoryStack.stackPush();

         try {
            int size = GL20C.glGetProgrami(vanillaProgram.getId(), 35717);
            Int2ObjectMap<String> sources = new Int2ObjectArrayMap(size);
            IntBuffer shaders = stack.mallocInt(size);
            GL20C.glGetAttachedShaders(vanillaProgram.getId(), null, shaders);

            for (int i = 0; i < shaders.limit(); i++) {
               int shader = shaders.get(i);
               int type = GL20C.glGetShaderi(shader, 35663);
               sources.put(type, GL20C.glGetShaderSource(shader));
            }

            this.dynamicPrograms
               .put(
                  name,
                  VeilRenderSystem.renderer()
                     .getShaderManager()
                     .createDynamicProgram(Sable.sablePath("dynamic_sublevel/" + name), sources)
                     .thenApplyAsync(shaderx -> {
                        ShaderUniform sableEnableNormalLighting = shaderx.getUniform("SableEnableNormalLighting");
                        if (sableEnableNormalLighting != null) {
                           sableEnableNormalLighting.setFloat(1.0F);
                        }

                        ShaderUniform sableEnableSkyLightShadows = shaderx.getUniform("SableShadowsEnabled");
                        if (sableEnableSkyLightShadows != null) {
                           sableEnableSkyLightShadows.setFloat(SableSkyLightShadows.isEnabled() ? 1.0F : 0.0F);
                        }

                        return shaderx;
                     }, Minecraft.getInstance())
               );
         } catch (Throwable var12) {
            if (stack != null) {
               try {
                  stack.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (stack != null) {
            stack.close();
         }

         return null;
      }
   }

   @Override
   public void rebuild(Iterable<ClientSubLevel> sublevels) {
      this.sectionCompiler.getBuffer().clear();
      SubLevelRenderDispatcher.super.rebuild(sublevels);
   }

   @Override
   public void updateCulling(Iterable<ClientSubLevel> sublevels, double cameraX, double cameraY, double cameraZ, CullFrustum cullFrustum, boolean isSpectator) {
      MutableBlockPos pos = new MutableBlockPos();

      for (ClientSubLevel subLevel : sublevels) {
         FancySubLevelRenderData renderData = (FancySubLevelRenderData)subLevel.getRenderData();
         Pose3dc renderPose = subLevel.renderPose();
         Vector3d plotPos = renderPose.transformPositionInverse(new Vector3d(cameraX, cameraY, cameraZ));
         Vector3ic chunkOrigin = renderData.getChunkOrigin();
         pos.set(plotPos.x, plotPos.y, plotPos.z);
         ClientLevel level = subLevel.getLevel();
         boolean smartCull = Minecraft.getInstance().smartCull;
         if (isSpectator && level.getBlockState(pos).isSolidRender(level, pos)) {
            smartCull = false;
         }

         renderData.getOcclusionData()
            .update((pos.getX() >> 4) - chunkOrigin.x(), (pos.getY() >> 4) - chunkOrigin.y(), (pos.getZ() >> 4) - chunkOrigin.z(), smartCull, cullFrustum);
      }
   }

   @Override
   public void renderSectionLayer(
      Iterable<ClientSubLevel> sublevels,
      RenderType renderType,
      ShaderInstance shader,
      double cameraX,
      double cameraY,
      double cameraZ,
      Matrix4f modelView,
      Matrix4f projection,
      float partialTicks
   ) {
      ShaderProgram program = this.getDynamicProgram(shader);
      if (program != null) {
         if (program.isValid()) {
            boolean setup = false;
            SubLevelTextureCache textureCache = this.sectionCompiler.getTextureCache();
            ShaderUniform sableSkyLightScale = program.getUniform("SableSkyLightScale");
            ShaderUniform sableTransform = program.getUniform("SableTransform");
            this.stagingBuffer.updateFencedAreas();

            for (ClientSubLevel subLevel : sublevels) {
               FancySubLevelRenderData renderData = (FancySubLevelRenderData)subLevel.getRenderData();
               FancySubLevelOcclusionData occlusionData = renderData.getOcclusionData();
               if (occlusionData.hasLayer(renderType)) {
                  if (!setup) {
                     program.bind();
                     program.setDefaultUniforms(Mode.TRIANGLES, modelView, projection);
                     program.bindSamplers(0);
                     textureCache.bind();
                     this.vertexArray.bind();
                     this.sectionCompiler.getBuffer().bind(this.vertexArray);
                     this.commandBuilder.setup();
                     setup = true;
                  }

                  if (sableSkyLightScale != null) {
                     int skyLight = subLevel.getLatestSkyLightScale();
                     sableSkyLightScale.setFloat((float)skyLight / 15.0F);
                  }

                  Pose3dc renderPose = subLevel.renderPose();
                  Vector3dc renderPos = renderPose.position();
                  Quaterniondc renderRot = renderPose.orientation();
                  Vector3d renderCOR = renderRot.transform(new Vector3d(renderPose.rotationPoint()).sub(renderData.getOrigin()));
                  if (sableTransform != null) {
                     Matrix4f transform = TRANSFORM.identity();
                     transform.translate(
                        (float)(renderPos.x() - renderCOR.x - cameraX),
                        (float)(renderPos.y() - renderCOR.y - cameraY),
                        (float)(renderPos.z() - renderCOR.z - cameraZ)
                     );
                     transform.rotate(new Quaternionf(renderRot));
                     sableTransform.setMatrix(transform);
                  }

                  Vector3d plotPos = renderPose.transformPositionInverse(new Vector3d(VeilRenderSystem.getCullingFrustum().getPosition()));
                  this.commandBuilder.draw(renderData, renderType, Mth.floor(plotPos.x) >> 4, Mth.floor(plotPos.y) >> 4, Mth.floor(plotPos.z) >> 4);
               }
            }

            this.stagingBuffer.updateFencedAreas();
            if (setup) {
               this.commandBuilder.clear();
            }
         }
      }
   }

   @Override
   public void renderAfterSections(
      Iterable<ClientSubLevel> sublevels, double cameraX, double cameraY, double cameraZ, Matrix4f modelView, Matrix4f projection, float partialTicks
   ) {
   }

   @Override
   public void renderBlockEntities(
      Iterable<ClientSubLevel> sublevels,
      SubLevelRenderDispatcher.BlockEntityRenderer blockEntityRenderer,
      double cameraX,
      double cameraY,
      double cameraZ,
      float partialTick
   ) {
   }

   @Override
   public void addDebugInfo(Consumer<String> consumer) {
      consumer.accept(
         "Staging Buffer: Used %.1f / %d MiB"
            .formatted((double)(this.stagingBuffer.getUsedSize() / 1024L) / 1024.0, this.stagingBuffer.getSize() / 1024L / 1024L)
      );
   }

   private void freePrograms() {
      for (CompletableFuture<ShaderProgram> future : this.dynamicPrograms.values()) {
         future.thenAcceptAsync(NativeResource::free, Minecraft.getInstance());
      }

      this.dynamicPrograms.clear();
   }

   public void free() {
      this.commandBuilder.free();
      this.sectionCompiler.free();
      this.stagingBuffer.free();
      this.vertexArray.free();
      this.freePrograms();
   }
}
