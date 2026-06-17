package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla.water_occlusion;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.SableClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/RenderBuffers;crumblingBufferSource()Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;",
         ordinal = 2,
         shift = Shift.AFTER
      )}
   )
   public void sable$preRenderSectionLayers(
      DeltaTracker deltaTracker,
      boolean bl,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      SableClient.WATER_OCCLUSION_RENDERER.preRenderTranslucent(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
   }

   @Inject(
      method = {"renderSectionLayer"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ShaderInstance;apply()V",
         shift = Shift.BEFORE
      )}
   )
   private void sable$onRenderSectionLayer(
      RenderType renderType, double d, double e, double f, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci, @Local ShaderInstance shader
   ) {
      if (renderType == RenderType.translucent()) {
         SableClient.WATER_OCCLUSION_RENDERER.setupTranslucentShader(shader);
      }
   }
}
