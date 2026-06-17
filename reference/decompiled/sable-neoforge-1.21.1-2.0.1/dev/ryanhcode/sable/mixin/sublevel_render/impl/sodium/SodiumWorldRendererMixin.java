package dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.rendertype.VeilRenderType.LayeredRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderType.RenderTypeWrapper;
import java.util.List;
import java.util.Objects;
import net.caffeinemc.mods.sodium.client.SodiumClientMod;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.caffeinemc.mods.sodium.client.render.chunk.TaskQueueType;
import net.caffeinemc.mods.sodium.client.render.viewport.Viewport;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {SodiumWorldRenderer.class},
   remap = false
)
public abstract class SodiumWorldRendererMixin {
   @Shadow
   private ClientLevel level;

   @ModifyReturnValue(
      method = {"getVisibleChunkCount"},
      at = {@At("RETURN")}
   )
   public int getVisibleChunkCount(int original) {
      int sum = original;

      for (ClientSubLevel sublevel : SubLevelContainer.getContainer(this.level).getAllSubLevels()) {
         sum += sublevel.getRenderData().getVisibleSectionCount();
      }

      return sum;
   }

   @Inject(
      method = {"setupTerrain"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSectionManager;markGraphDirty()V"
      )}
   )
   public void sable$markGraphDirty(Camera camera, Viewport viewport, boolean spectator, boolean updateChunksImmediately, CallbackInfo ci) {
      Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer)((SubLevelContainerHolder)this.level).sable$getPlotContainer()).getAllSubLevels();
      Vec3 cameraPosition = camera.getPosition();
      Minecraft minecraft = Minecraft.getInstance();
      Frustum frustum = minecraft.levelRenderer.cullingFrustum;
      SubLevelRenderDispatcher.get()
         .updateCulling(sublevels, cameraPosition.x, cameraPosition.y, cameraPosition.z, VeilRenderBridge.create(frustum), minecraft.player.isSpectator());
   }

   @Inject(
      method = {"setupTerrain"},
      at = {@At("TAIL")}
   )
   public void sable$setupTerrain(Camera camera, Viewport viewport, boolean spectator, boolean updateChunksImmediately, CallbackInfo ci) {
      SubLevelRenderDispatcher dispatcher = SubLevelRenderDispatcher.get();
      dispatcher.preRenderChunks(camera);
      Iterable<ClientSubLevel> sublevels = SubLevelContainer.getContainer(this.level).getAllSubLevels();
      RenderRegionCache renderRegionCache = new RenderRegionCache();
      TaskQueueType buildQueueType = SodiumClientMod.options().performance.chunkBuildDeferMode.getImportantRebuildQueueType();
      PrioritizeChunkUpdates chunkUpdates = buildQueueType == TaskQueueType.ALWAYS_DEFER ? PrioritizeChunkUpdates.NONE : PrioritizeChunkUpdates.NEARBY;

      for (ClientSubLevel sublevel : sublevels) {
         sublevel.getRenderData().compileSections(chunkUpdates, renderRegionCache, camera);
      }
   }

   @Inject(
      method = {"scheduleRebuildForChunk(IIIZ)V"},
      at = {@At("TAIL")}
   )
   public void sable$scheduleRebuildForChunk(int x, int y, int z, boolean playerChanged, CallbackInfo ci) {
      ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
      if (container != null && container.inBounds(x, z)) {
         ClientSubLevel subLevel = (ClientSubLevel)Sable.HELPER.getContaining(this.level, new ChunkPos(x, z));
         if (subLevel != null) {
            subLevel.getRenderData().setDirty(x, y, z, playerChanged);
         }
      }
   }

   @Inject(
      method = {"drawChunkLayer"},
      at = {@At("TAIL")}
   )
   public void sable$drawRenderSources(RenderType renderType, ChunkRenderMatrices matrices, double camX, double camY, double camZ, CallbackInfo ci) {
      SubLevelRenderDispatcher renderDispatcher = SubLevelRenderDispatcher.get();
      Minecraft minecraft = Minecraft.getInstance();
      float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
      List<ClientSubLevel> subLevels = SubLevelContainer.getContainer(this.level).getAllSubLevels();
      Matrix4f modelView = new Matrix4f(matrices.modelView());
      Matrix4f projection = new Matrix4f(matrices.projection());
      renderType.setupRenderState();
      ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader(), "shader");
      shader.setDefaultUniforms(Mode.QUADS, modelView, projection, minecraft.getWindow());
      shader.apply();
      renderDispatcher.renderSectionLayer(subLevels, renderType, shader, camX, camY, camZ, modelView, projection, partialTicks);
      shader.clear();
      renderType.clearRenderState();
      RenderType unwrappedRenderType = renderType;

      while (unwrappedRenderType instanceof RenderTypeWrapper) {
         RenderTypeWrapper wrapper = (RenderTypeWrapper)unwrappedRenderType;
         unwrappedRenderType = wrapper.get();
      }

      if (unwrappedRenderType instanceof LayeredRenderType layered) {
         for (RenderType layer : layered.getLayers()) {
            layer.setupRenderState();
            ShaderInstance shaderx = Objects.requireNonNull(RenderSystem.getShader(), "shader");
            shaderx.setDefaultUniforms(Mode.QUADS, modelView, projection, minecraft.getWindow());
            shaderx.apply();
            renderDispatcher.renderSectionLayer(subLevels, layer, shaderx, camX, camY, camZ, modelView, projection, partialTicks);
            shaderx.clear();
            layer.clearRenderState();
         }
      }
   }
}
