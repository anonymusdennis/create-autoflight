package dev.engine_room.flywheel.impl.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.impl.FlwImplXplat;
import dev.engine_room.flywheel.impl.event.RenderContextImpl;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {LevelRenderer.class},
   priority = 1001
)
abstract class LevelRendererMixin {
   @Shadow
   @Nullable
   private ClientLevel level;
   @Shadow
   @Final
   private RenderBuffers renderBuffers;
   @Shadow
   @Final
   private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;
   @Unique
   @Nullable
   private RenderContextImpl flywheel$renderContext;

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE_ASSIGN",
         target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;runLightUpdates()I"
      )}
   )
   private void flywheel$beginRender(
      DeltaTracker deltaTracker,
      boolean renderBlockOutline,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f modelMatrix,
      Matrix4f projectionMatrix,
      CallbackInfo ci
   ) {
      this.flywheel$renderContext = RenderContextImpl.create(
         (LevelRenderer)this, this.level, this.renderBuffers, modelMatrix, projectionMatrix, camera, deltaTracker.getGameTimeDeltaPartialTick(false)
      );
      VisualizationManager manager = VisualizationManager.get(this.level);
      if (manager != null) {
         manager.renderDispatcher().onStartLevelRender(this.flywheel$renderContext);
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At("RETURN")}
   )
   private void flywheel$endRender(CallbackInfo ci) {
      this.flywheel$renderContext = null;
   }

   @Inject(
      method = {"allChanged"},
      at = {@At("RETURN")}
   )
   private void flywheel$reload(CallbackInfo ci) {
      if (this.level != null) {
         FlwImplXplat.INSTANCE.dispatchReloadLevelRendererEvent(this.level);
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE_STRING",
         target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
         args = {"ldc=blockentities"}
      )}
   )
   private void flywheel$beforeBlockEntities(CallbackInfo ci) {
      if (this.flywheel$renderContext != null) {
         VisualizationManager manager = VisualizationManager.get(this.level);
         if (manager != null) {
            manager.renderDispatcher().afterEntities(this.flywheel$renderContext);
         }
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE_STRING",
         target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
         args = {"ldc=destroyProgress"}
      )}
   )
   private void flywheel$beforeRenderCrumbling(CallbackInfo ci) {
      if (this.flywheel$renderContext != null) {
         VisualizationManager manager = VisualizationManager.get(this.level);
         if (manager != null) {
            manager.renderDispatcher().beforeCrumbling(this.flywheel$renderContext, this.destructionProgress);
         }
      }
   }

   @Inject(
      method = {"renderEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void flywheel$decideNotToRenderEntity(
      Entity entity, double camX, double camY, double camZ, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, CallbackInfo ci
   ) {
      if (VisualizationManager.supportsVisualization(entity.level()) && VisualizationHelper.skipVanillaRender(entity)) {
         ci.cancel();
      }
   }
}
