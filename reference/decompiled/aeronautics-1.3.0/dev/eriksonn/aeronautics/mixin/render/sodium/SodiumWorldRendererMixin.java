package dev.eriksonn.aeronautics.mixin.render.sodium;

import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager;
import dev.eriksonn.aeronautics.index.client.AeroRenderTypes;
import foundry.veil.api.client.render.VeilRenderBridge;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniform;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {SodiumWorldRenderer.class},
   priority = 990
)
public class SodiumWorldRendererMixin {
   @Inject(
      method = {"drawChunkLayer"},
      at = {@At("HEAD")}
   )
   public void aeronautics$setupLevititeShaders(RenderType renderType, ChunkRenderMatrices matrices, double x, double y, double z, CallbackInfo ci) {
      if (renderType == AeroRenderTypes.levitite()) {
         ShaderProgram shader = VeilRenderSystem.setShader(AeroRenderTypes.LEVITITE_SHADER);
         if (shader == null) {
            return;
         }

         ShaderUniform time = shader.getUniform("time");
         if (time != null) {
            long ticks = Minecraft.getInstance().level.getGameTime();
            float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
            ticks %= 100000L;
            time.setFloat((float)ticks + pt);
         }

         LevititeShaderManager.prepareShaderForWorld(VeilRenderBridge.toShaderInstance(shader), x, y, z);
      }
   }

   @Inject(
      method = {"drawChunkLayer"},
      at = {@At("TAIL")}
   )
   public void aeronautics$cleanupLevititeShaders(RenderType renderLayer, ChunkRenderMatrices matrices, double x, double y, double z, CallbackInfo ci) {
      if (renderLayer == AeroRenderTypes.levitite()) {
         ShaderProgram shader = VeilRenderSystem.setShader(AeroRenderTypes.LEVITITE_SHADER);
         if (shader == null) {
            return;
         }

         LevititeShaderManager.prepareShaderForWorld(VeilRenderBridge.toShaderInstance(shader), x, y, z);
      }
   }
}
