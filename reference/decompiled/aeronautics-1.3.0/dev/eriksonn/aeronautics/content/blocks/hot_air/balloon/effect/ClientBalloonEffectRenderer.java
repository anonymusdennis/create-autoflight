package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ClientBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.post.PostPipeline;
import foundry.veil.api.client.render.post.PostProcessingManager;
import foundry.veil.api.client.render.post.PostPipeline.Context;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniformAccess;
import foundry.veil.api.event.VeilRenderLevelStageEvent.Stage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL30;

public class ClientBalloonEffectRenderer {
   private static final ResourceLocation FBO_ID = Aeronautics.path("soft_light");
   private static final ResourceLocation POST_SHADER_ID = Aeronautics.path("soft_light");
   private static final ResourceLocation SIDE_TEXTURE = Aeronautics.path("textures/special/heat_overlay.png");
   private static final ResourceLocation TOP_TEXTURE = Aeronautics.path("textures/special/lava_still.png");
   private static final ResourceLocation SHADER_ID = Aeronautics.path("hot_air_overlay");
   @Nullable
   private static AdvancedFbo overlayFbo;

   public static void onRenderLevelStage(Stage stage, Matrix4fc frustumMatrix, Matrix4fc projectionMatrix, int renderTick) {
      if (stage == Stage.AFTER_SOLID_BLOCKS) {
         Minecraft minecraft = Minecraft.getInstance();
         ClientLevel level = minecraft.level;
         if (level == null) {
            freeFbo();
         } else {
            BalloonMap ballonMap = (BalloonMap)BalloonMap.MAP.get(level);
            if (ballonMap.isEmpty()) {
               freeFbo();
            } else {
               Window window = minecraft.getWindow();
               if (overlayFbo == null || overlayFbo.getWidth() != window.getWidth() || overlayFbo.getHeight() != window.getHeight()) {
                  freeFbo();
                  overlayFbo = AdvancedFbo.withSize(window.getWidth(), window.getHeight()).addColorTextureBuffer().setDepthTextureBuffer().build(true);
               }

               renderBalloonEffects(ballonMap, frustumMatrix, projectionMatrix, renderTick);
            }
         }
      }
   }

   private static void renderBalloonEffects(BalloonMap balloonMap, Matrix4fc frustumMatrix, Matrix4fc projectionMatrix, int renderTick) {
      Minecraft minecraft = Minecraft.getInstance();
      float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
      ShaderProgram shader = VeilRenderSystem.setShader(SHADER_ID);
      if (shader != null) {
         overlayFbo.bind(false);
         overlayFbo.clear(0.0F, 0.0F, 0.0F, 0.0F, 16640);
         RenderSystem.setShaderTexture(0, SIDE_TEXTURE);
         RenderSystem.setShaderTexture(1, TOP_TEXTURE);
         RenderSystem.enableCull();
         RenderSystem.depthMask(true);
         RenderSystem.enableDepthTest();
         GL30.glCullFace(1028);
         RenderSystem.polygonOffset(-0.5F, -30.0F);
         RenderSystem.enablePolygonOffset();
         float scrollAmount = ((float)renderTick + partialTicks) / -20.0F;
         ShaderUniformAccess scrollUniform = shader.getUniformSafe("Scroll");
         ShaderUniformAccess yCutoffUniform = shader.getUniformSafe("CutoffY");
         scrollUniform.setFloat((float)(Math.floor((double)(scrollAmount * 16.0F)) / 16.0));
         float brightness = 0.85F;
         float alpha = 1.0F;
         RenderSystem.setShaderColor(0.85F, 0.85F, 0.85F, 1.0F);
         Matrix4f modelViewMat = new Matrix4f(frustumMatrix);
         Matrix4f projMat = new Matrix4f(projectionMatrix);

         for (Balloon balloon : balloonMap.getBalloons()) {
            ClientBalloon clientBalloon = (ClientBalloon)balloon;
            HeatedCulledRenderRegion renderRegion = clientBalloon.getRenderRegion();
            if (renderRegion != null) {
               float filledPercent = 0.0F;

               for (BlockEntityLiftingGasProvider heater : balloon.getHeaters()) {
                  filledPercent = Math.max(filledPercent, (float)heater.getClientPredictedVolume() / (float)balloon.getCapacity());
               }

               filledPercent = Mth.clamp(filledPercent, 0.0F, 1.0F);
               yCutoffUniform.setFloat((1.0F - filledPercent) * (balloon.getHeight() + 1.0F));
               renderRegion.render(modelViewMat, projMat);
            }
         }

         RenderSystem.polygonOffset(0.0F, 0.0F);
         RenderSystem.disablePolygonOffset();
         GL30.glCullFace(1029);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         AdvancedFbo.unbind();
         applyHeatingToScreen();
      }
   }

   private static void applyHeatingToScreen() {
      PostProcessingManager manager = VeilRenderSystem.renderer().getPostProcessingManager();
      PostPipeline pipeline = manager.getPipeline(POST_SHADER_ID);
      Context context = manager.getPostPipelineContext();
      context.setFramebuffer(FBO_ID, overlayFbo);
      manager.runPipeline(pipeline);
   }

   private static void freeFbo() {
      if (overlayFbo != null) {
         overlayFbo.free();
      }

      overlayFbo = null;
   }
}
