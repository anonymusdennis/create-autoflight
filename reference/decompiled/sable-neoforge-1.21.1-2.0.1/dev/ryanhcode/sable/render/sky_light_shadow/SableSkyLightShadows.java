package dev.ryanhcode.sable.render.sky_light_shadow;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.event.VeilRenderLevelStageEvent.Stage;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL30;

public class SableSkyLightShadows {
   public static final float SHADOW_VOLUME_SIZE = 128.0F;
   private static final ResourceLocation FRAMEBUFFER_NAME = Sable.sablePath("sub_level_shadow");
   private static final Matrix4f PROJECTION_MAT = new Matrix4f();
   private static final Vector3d SHADOW_CAMERA_POSITION = new Vector3d();
   private static final Quaternionf SHADOW_CAMERA_ORIENTATION = new Quaternionf();
   private static boolean isRenderingShadowMap = false;
   private static boolean isEnabled = false;

   public static boolean isEnabled() {
      return isEnabled;
   }

   public static void setIsEnabled(boolean isEnabled) {
      SableSkyLightShadows.isEnabled = isEnabled;
   }

   public static void renderShadowMap(
      Stage stage,
      LevelRenderer levelRenderer,
      BufferSource bufferSource,
      MatrixStack matrixStack,
      Matrix4fc frustumMatrix,
      Matrix4fc projectionMatrix,
      int renderTick,
      DeltaTracker deltaTracker,
      Camera camera,
      Frustum frustum
   ) {
      if (isEnabled()) {
         if (!VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            if (stage == Stage.AFTER_LEVEL) {
               AdvancedFbo fbo = getShadowsFramebuffer();
               if (fbo != null) {
                  fbo.bind(true);
                  GL30.glClearColor(1.0F, 1.0F, 1.0F, 0.0F);
                  fbo.clear();
                  Minecraft client = Minecraft.getInstance();
                  Level level = client.level;
                  Window window = client.getWindow();
                  Matrix4f modelView = new Matrix4f();
                  PROJECTION_MAT.identity().ortho(-128.0F, 128.0F, -128.0F, 128.0F, 0.5F, 128.0F);
                  Vec3 cameraPosition = camera.getPosition();
                  Vec3 shadowCameraPosition = new Vec3(cameraPosition.x, cameraPosition.y + 64.0, cameraPosition.z);
                  JOMLConversion.toJOML(shadowCameraPosition, SHADOW_CAMERA_POSITION);
                  SHADOW_CAMERA_POSITION.set(Math.floor(SHADOW_CAMERA_POSITION.x), SHADOW_CAMERA_POSITION.y, Math.floor(SHADOW_CAMERA_POSITION.z));
                  isRenderingShadowMap = true;
                  VeilLevelPerspectiveRenderer.render(
                     fbo,
                     modelView,
                     PROJECTION_MAT,
                     SHADOW_CAMERA_POSITION,
                     SHADOW_CAMERA_ORIENTATION.identity().rotateX((float) (Math.PI / 2)),
                     8.0F,
                     deltaTracker,
                     false
                  );
                  isRenderingShadowMap = false;
               }
            }
         }
      }
   }

   @Nullable
   public static AdvancedFbo getShadowsFramebuffer() {
      return VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(FRAMEBUFFER_NAME);
   }

   public static boolean renderingShadowMap() {
      return isRenderingShadowMap;
   }

   public static void bindShadowMapTexture(ShaderInstance shader) {
      if (isEnabled()) {
         Uniform volumeSizeUniform = shader.getUniform("SableShadowVolumeSize");
         if (volumeSizeUniform != null) {
            volumeSizeUniform.set(128.0F);
         }

         Uniform offsetUniform = shader.getUniform("SableShadowOrigin");
         if (offsetUniform != null) {
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            offsetUniform.set(
               (float)(SHADOW_CAMERA_POSITION.x - camera.x), (float)(SHADOW_CAMERA_POSITION.y - camera.y), (float)(SHADOW_CAMERA_POSITION.z - camera.z)
            );
         }

         AdvancedFbo fbo = getShadowsFramebuffer();
         shader.setSampler("SableShadowSampler", fbo.getDepthTextureAttachment());
      }
   }
}
