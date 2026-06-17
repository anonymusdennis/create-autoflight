package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.rendertype.VeilRenderType.LayeredRenderType;
import foundry.veil.api.client.render.rendertype.VeilRenderType.RenderTypeWrapper;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {LevelRenderer.class},
   priority = 1002
)
public abstract class LevelRendererMixin {
   @Shadow
   @Nullable
   private ClientLevel level;
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"compileSections"},
      at = {@At("TAIL")}
   )
   private void sable$compileSections(Camera camera, CallbackInfo ci) {
      Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer)((SubLevelContainerHolder)this.level).sable$getPlotContainer()).getAllSubLevels();
      RenderRegionCache renderRegionCache = new RenderRegionCache();
      PrioritizeChunkUpdates chunkUpdates = (PrioritizeChunkUpdates)Minecraft.getInstance().options.prioritizeChunkUpdates().get();

      for (ClientSubLevel sublevel : sublevels) {
         sublevel.getRenderData().compileSections(chunkUpdates, renderRegionCache, camera);
      }
   }

   @Inject(
      method = {"setupRender"},
      at = {@At(
         value = "INVOKE_STRING",
         target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
         args = {"ldc=update"}
      )}
   )
   public void sable$cull(Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean isSpectator, CallbackInfo ci) {
      if (!hasCapturedFrustum) {
         SubLevelRenderDispatcher dispatcher = SubLevelRenderDispatcher.get();
         dispatcher.preRenderChunks(camera);
         ProfilerFiller profiler = this.minecraft.getProfiler();
         profiler.push("sub_level_section_occlusion_graph");
         Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer)((SubLevelContainerHolder)this.level).sable$getPlotContainer()).getAllSubLevels();
         Vec3 cameraPosition = camera.getPosition();
         dispatcher.updateCulling(sublevels, cameraPosition.x, cameraPosition.y, cameraPosition.z, VeilRenderBridge.create(frustum), isSpectator);
         profiler.pop();
      }
   }

   @Inject(
      method = {"isSectionCompiled"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$isSectionCompiled(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
      ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
      if (container != null) {
         if (container.inBounds(blockPos)) {
            ClientSubLevel subLevel = (ClientSubLevel)Sable.HELPER.getContaining(this.level, blockPos);
            if (subLevel == null) {
               cir.setReturnValue(false);
            } else {
               SubLevelRenderData renderData = subLevel.getRenderData();
               SectionPos sectionPos = SectionPos.of(blockPos);
               cir.setReturnValue(renderData.isSectionCompiled(sectionPos.x(), sectionPos.y(), sectionPos.z()));
            }
         }
      }
   }

   @Inject(
      method = {"renderSectionLayer"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ShaderInstance;clear()V"
      )}
   )
   public void sable$renderSubLevels(
      RenderType renderType, double x, double y, double z, Matrix4f modelView, Matrix4f projection, CallbackInfo ci, @Local ShaderInstance shader
   ) {
      Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer)((SubLevelContainerHolder)this.level).sable$getPlotContainer()).getAllSubLevels();
      SubLevelRenderDispatcher.get()
         .renderSectionLayer(
            sublevels, renderType, shader, x, y, z, modelView, projection, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)
         );
   }

   @Inject(
      method = {"renderSectionLayer"},
      at = {@At("TAIL")}
   )
   public void sable$renderSubLevelLayers(RenderType renderType, double x, double y, double z, Matrix4f modelView, Matrix4f projection, CallbackInfo ci) {
      RenderType unwrappedRenderType = renderType;

      while (unwrappedRenderType instanceof RenderTypeWrapper) {
         RenderTypeWrapper wrapper = (RenderTypeWrapper)unwrappedRenderType;
         unwrappedRenderType = wrapper.get();
      }

      if (unwrappedRenderType instanceof LayeredRenderType layered) {
         List sublevels = ((ClientSubLevelContainer)((SubLevelContainerHolder)this.level).sable$getPlotContainer()).getAllSubLevels();
         SubLevelRenderDispatcher renderDispatcher = SubLevelRenderDispatcher.get();

         for (RenderType layer : layered.getLayers()) {
            layer.setupRenderState();
            ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader(), "shader");
            shader.setDefaultUniforms(Mode.QUADS, modelView, projection, this.minecraft.getWindow());
            shader.apply();
            renderDispatcher.renderSectionLayer(
               sublevels, renderType, shader, x, y, z, modelView, projection, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)
            );
            shader.clear();
            layer.clearRenderState();
         }
      }
   }
}
