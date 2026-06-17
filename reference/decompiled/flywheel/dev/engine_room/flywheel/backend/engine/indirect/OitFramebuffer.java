package dev.engine_room.flywheel.backend.engine.indirect;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.engine_room.flywheel.backend.NoiseTextures;
import dev.engine_room.flywheel.backend.Samplers;
import dev.engine_room.flywheel.backend.compile.OitPrograms;
import dev.engine_room.flywheel.backend.gl.GlCompat;
import dev.engine_room.flywheel.backend.gl.GlTextureUnit;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL46;

public class OitFramebuffer {
   public static final float[] CLEAR_TO_ZERO = new float[]{0.0F, 0.0F, 0.0F, 0.0F};
   public static final int[] DEPTH_RANGE_DRAW_BUFFERS = new int[]{36064};
   public static final int[] RENDER_TRANSMITTANCE_DRAW_BUFFERS = new int[]{36065, 36066, 36067, 36068};
   public static final int[] ACCUMULATE_DRAW_BUFFERS = new int[]{36069};
   public static final int[] DEPTH_ONLY_DRAW_BUFFERS = new int[0];
   private final OitPrograms programs;
   private final int vao;
   public int fbo = -1;
   public int depthBounds = -1;
   public int coefficients = -1;
   public int accumulate = -1;
   private int lastWidth = -1;
   private int lastHeight = -1;

   public OitFramebuffer(OitPrograms programs) {
      this.programs = programs;
      if (GlCompat.SUPPORTS_DSA) {
         this.vao = GL46.glCreateVertexArrays();
      } else {
         this.vao = GL32.glGenVertexArrays();
      }
   }

   public void prepare() {
      RenderTarget renderTarget;
      if (Minecraft.useShaderTransparency()) {
         renderTarget = Minecraft.getInstance().levelRenderer.getItemEntityTarget();
         renderTarget.copyDepthFrom(Minecraft.getInstance().getMainRenderTarget());
      } else {
         renderTarget = Minecraft.getInstance().getMainRenderTarget();
      }

      this.maybeResizeFBO(renderTarget.width, renderTarget.height);
      Samplers.COEFFICIENTS.makeActive();
      RenderSystem.bindTexture(0);
      GL32.glBindTexture(35866, this.coefficients);
      Samplers.DEPTH_RANGE.makeActive();
      RenderSystem.bindTexture(this.depthBounds);
      Samplers.NOISE.makeActive();
      NoiseTextures.BLUE_NOISE.bind();
      GlStateManager._glBindFramebuffer(36160, this.fbo);
      GL32.glFramebufferTexture(36160, 36096, renderTarget.getDepthTextureId(), 0);
   }

   public void depthRange() {
      RenderSystem.depthMask(false);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
      RenderSystem.blendEquation(32776);
      float far = Minecraft.getInstance().gameRenderer.getDepthFar();
      if (GlCompat.SUPPORTS_DSA) {
         GL46.glNamedFramebufferDrawBuffers(this.fbo, DEPTH_RANGE_DRAW_BUFFERS);
         GL46.glClearNamedFramebufferfv(this.fbo, 6144, 0, new float[]{-far, -far, 0.0F, 0.0F});
      } else {
         GL32.glDrawBuffers(DEPTH_RANGE_DRAW_BUFFERS);
         RenderSystem.clearColor(-far, -far, 0.0F, 0.0F);
         RenderSystem.clear(16384, false);
      }
   }

   public void renderTransmittance() {
      RenderSystem.depthMask(false);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
      RenderSystem.blendEquation(32774);
      if (GlCompat.SUPPORTS_DSA) {
         GL46.glNamedFramebufferDrawBuffers(this.fbo, RENDER_TRANSMITTANCE_DRAW_BUFFERS);
         GL46.glClearNamedFramebufferfv(this.fbo, 6144, 0, CLEAR_TO_ZERO);
         GL46.glClearNamedFramebufferfv(this.fbo, 6144, 1, CLEAR_TO_ZERO);
         GL46.glClearNamedFramebufferfv(this.fbo, 6144, 2, CLEAR_TO_ZERO);
         GL46.glClearNamedFramebufferfv(this.fbo, 6144, 3, CLEAR_TO_ZERO);
      } else {
         GL32.glDrawBuffers(RENDER_TRANSMITTANCE_DRAW_BUFFERS);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16384, false);
      }
   }

   public void renderDepthFromTransmittance() {
      RenderSystem.depthMask(true);
      RenderSystem.colorMask(false, false, false, false);
      RenderSystem.disableBlend();
      RenderSystem.depthFunc(519);
      if (GlCompat.SUPPORTS_DSA) {
         GL46.glNamedFramebufferDrawBuffers(this.fbo, DEPTH_ONLY_DRAW_BUFFERS);
      } else {
         GL32.glDrawBuffers(DEPTH_ONLY_DRAW_BUFFERS);
      }

      this.programs.getOitDepthProgram().bind();
      this.drawFullscreenQuad();
   }

   public void accumulate() {
      RenderSystem.depthMask(false);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE);
      RenderSystem.blendEquation(32774);
      if (GlCompat.SUPPORTS_DSA) {
         GL46.glNamedFramebufferDrawBuffers(this.fbo, ACCUMULATE_DRAW_BUFFERS);
         GL46.glClearNamedFramebufferfv(this.fbo, 6144, 0, CLEAR_TO_ZERO);
      } else {
         GL32.glDrawBuffers(ACCUMULATE_DRAW_BUFFERS);
         RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
         RenderSystem.clear(16384, false);
      }
   }

   public void composite() {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().levelRenderer.getItemEntityTarget().bindWrite(false);
      } else {
         Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
      }

      RenderSystem.depthMask(true);
      RenderSystem.colorMask(true, true, true, true);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.blendEquation(32774);
      RenderSystem.depthFunc(519);
      GlTextureUnit.T0.makeActive();
      RenderSystem.bindTexture(this.accumulate);
      this.programs.getOitCompositeProgram().bind();
      this.drawFullscreenQuad();
      Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
   }

   public void delete() {
      this.deleteTextures();
      GL32.glDeleteVertexArrays(this.vao);
   }

   private void drawFullscreenQuad() {
      GlStateManager._glBindVertexArray(this.vao);
      GL32.glDrawArrays(4, 0, 3);
   }

   private void deleteTextures() {
      if (this.depthBounds != -1) {
         GL32.glDeleteTextures(this.depthBounds);
      }

      if (this.coefficients != -1) {
         GL32.glDeleteTextures(this.coefficients);
      }

      if (this.accumulate != -1) {
         GL32.glDeleteTextures(this.accumulate);
      }

      if (this.fbo != -1) {
         GL32.glDeleteFramebuffers(this.fbo);
      }

      Samplers.COEFFICIENTS.makeActive();
      RenderSystem.bindTexture(0);
      Samplers.DEPTH_RANGE.makeActive();
      RenderSystem.bindTexture(0);
   }

   private void maybeResizeFBO(int width, int height) {
      if (this.lastWidth != width || this.lastHeight != height) {
         this.lastWidth = width;
         this.lastHeight = height;
         this.deleteTextures();
         if (GlCompat.SUPPORTS_DSA) {
            this.fbo = GL46.glCreateFramebuffers();
            this.depthBounds = GL46.glCreateTextures(3553);
            this.coefficients = GL46.glCreateTextures(35866);
            this.accumulate = GL46.glCreateTextures(3553);
            GL46.glTextureStorage2D(this.depthBounds, 1, 33328, width, height);
            GL46.glTextureStorage3D(this.coefficients, 1, 34842, width, height, 4);
            GL46.glTextureStorage2D(this.accumulate, 1, 34842, width, height);
            GL46.glNamedFramebufferTexture(this.fbo, 36064, this.depthBounds, 0);
            GL46.glNamedFramebufferTextureLayer(this.fbo, 36065, this.coefficients, 0, 0);
            GL46.glNamedFramebufferTextureLayer(this.fbo, 36066, this.coefficients, 0, 1);
            GL46.glNamedFramebufferTextureLayer(this.fbo, 36067, this.coefficients, 0, 2);
            GL46.glNamedFramebufferTextureLayer(this.fbo, 36068, this.coefficients, 0, 3);
            GL46.glNamedFramebufferTexture(this.fbo, 36069, this.accumulate, 0);
         } else {
            this.fbo = GL46.glGenFramebuffers();
            this.depthBounds = GL32.glGenTextures();
            this.coefficients = GL32.glGenTextures();
            this.accumulate = GL32.glGenTextures();
            GlTextureUnit.T0.makeActive();
            RenderSystem.bindTexture(0);
            GL32.glBindTexture(3553, this.depthBounds);
            GL32.glTexImage2D(3553, 0, 33328, width, height, 0, 6408, 5120, 0L);
            GL32.glTexParameteri(3553, 10241, 9728);
            GL32.glTexParameteri(3553, 10240, 9728);
            GL32.glTexParameteri(3553, 10242, 33071);
            GL32.glTexParameteri(3553, 10243, 33071);
            GL32.glBindTexture(35866, this.coefficients);
            GL32.glTexImage3D(35866, 0, 34842, width, height, 4, 0, 6408, 5120, 0L);
            GL32.glTexParameteri(35866, 10241, 9728);
            GL32.glTexParameteri(35866, 10240, 9728);
            GL32.glTexParameteri(35866, 10242, 33071);
            GL32.glTexParameteri(35866, 10243, 33071);
            GL32.glBindTexture(3553, this.accumulate);
            GL32.glTexImage2D(3553, 0, 34842, width, height, 0, 6408, 5120, 0L);
            GL32.glTexParameteri(3553, 10241, 9728);
            GL32.glTexParameteri(3553, 10240, 9728);
            GL32.glTexParameteri(3553, 10242, 33071);
            GL32.glTexParameteri(3553, 10243, 33071);
            GlStateManager._glBindFramebuffer(36160, this.fbo);
            GL46.glFramebufferTexture(36160, 36064, this.depthBounds, 0);
            GL46.glFramebufferTextureLayer(36160, 36065, this.coefficients, 0, 0);
            GL46.glFramebufferTextureLayer(36160, 36066, this.coefficients, 0, 1);
            GL46.glFramebufferTextureLayer(36160, 36067, this.coefficients, 0, 2);
            GL46.glFramebufferTextureLayer(36160, 36068, this.coefficients, 0, 3);
            GL46.glFramebufferTexture(36160, 36069, this.accumulate, 0);
         }
      }
   }
}
