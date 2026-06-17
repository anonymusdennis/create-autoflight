package dev.eriksonn.aeronautics.index.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.levitite.LevititeShaderManager;
import foundry.veil.api.client.render.VeilRenderBridge;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.LightmapStateShard;
import net.minecraft.client.renderer.RenderStateShard.OutputStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public class AeroRenderTypes extends RenderType {
   public static final ResourceLocation LEVITITE_SHADER = Aeronautics.path("levitite/levitite");
   private static final ShaderStateShard LEVITITE_SHADER_SHARD = new AeroRenderTypes.LevititeShaderState(
      VeilRenderBridge.shaderState(LEVITITE_SHADER), new OutputStateShard("disabled", () -> {
         RENDERTYPE_SOLID_SHADER.setupRenderState();
         RenderSystem.colorMask(false, false, false, false);
         RenderSystem.depthMask(false);
      }, () -> {
         RENDERTYPE_SOLID_SHADER.clearRenderState();
         RenderSystem.colorMask(true, true, true, true);
         RenderSystem.depthMask(true);
      })
   );
   private static final RenderType LEVITITE = RenderType.create(
      "aeronautics:levitite",
      DefaultVertexFormat.BLOCK,
      Mode.QUADS,
      1536,
      false,
      true,
      VeilRenderBridge.create(
            CompositeState.builder()
               .setShaderState(LEVITITE_SHADER_SHARD)
               .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
               .setCullState(CULL)
               .setTextureState(RenderStateShard.BLOCK_SHEET)
               .setLightmapState(LightmapStateShard.LIGHTMAP)
         )
         .addLayer(VeilRenderBridge.patchState(4))
         .create(false)
   );
   private static final RenderType LEVITITE_GHOSTS = RenderType.create(
      "aeronautics:levitite_ghosts",
      DefaultVertexFormat.BLOCK,
      Mode.QUADS,
      1536,
      false,
      true,
      VeilRenderBridge.create(
            CompositeState.builder()
               .setShaderState(LEVITITE_SHADER_SHARD)
               .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
               .setCullState(NO_CULL)
               .setTextureState(RenderStateShard.BLOCK_SHEET)
               .setLightmapState(LightmapStateShard.LIGHTMAP)
         )
         .addLayer(VeilRenderBridge.patchState(4))
         .create(false)
   );

   public AeroRenderTypes(
      String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState
   ) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
   }

   public static RenderType levitite() {
      return LEVITITE;
   }

   public static RenderType levititeGhosts() {
      return LEVITITE_GHOSTS;
   }

   private static class LevititeShaderState extends ShaderStateShard {
      private final RenderStateShard enabled;
      private final RenderStateShard disabled;

      public LevititeShaderState(RenderStateShard enabled, RenderStateShard disabled) {
         this.enabled = enabled;
         this.disabled = disabled;
      }

      public void setupRenderState() {
         if (LevititeShaderManager.isEnabled()) {
            this.enabled.setupRenderState();
         } else {
            this.disabled.setupRenderState();
         }
      }

      public void clearRenderState() {
         if (LevititeShaderManager.isEnabled()) {
            this.enabled.clearRenderState();
         } else {
            this.disabled.clearRenderState();
         }
      }

      public String toString() {
         return LevititeShaderManager.isEnabled() ? this.enabled.toString() : this.disabled.toString();
      }
   }
}
