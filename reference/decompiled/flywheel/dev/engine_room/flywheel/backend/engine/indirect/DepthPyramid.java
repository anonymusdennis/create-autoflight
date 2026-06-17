package dev.engine_room.flywheel.backend.engine.indirect;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import dev.engine_room.flywheel.backend.compile.IndirectPrograms;
import dev.engine_room.flywheel.backend.gl.GlTextureUnit;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.lib.math.MoreMath;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL46;

public class DepthPyramid {
   private final IndirectPrograms programs;
   public int pyramidTextureId = -1;
   private int lastWidth = -1;
   private int lastHeight = -1;

   public DepthPyramid(IndirectPrograms programs) {
      this.programs = programs;
   }

   public void generate() {
      RenderTarget mainRenderTarget = Minecraft.getInstance().getMainRenderTarget();
      int width = mip0Size(mainRenderTarget.width);
      int height = mip0Size(mainRenderTarget.height);
      int mipLevels = getImageMipLevels(width, height);
      this.createPyramidMips(mipLevels, width, height);
      int depthBufferId = mainRenderTarget.getDepthTextureId();
      GL46.glMemoryBarrier(1024);
      GlTextureUnit.T0.makeActive();
      GlStateManager._bindTexture(depthBufferId);
      GlProgram downsampleFirstProgram = this.programs.getDownsampleFirstProgram();
      downsampleFirstProgram.bind();
      GL46.glBindImageTexture(1, this.pyramidTextureId, 0, false, 0, 35001, 33326);
      GL46.glDispatchCompute(MoreMath.ceilingDiv(width << 1, 64), MoreMath.ceilingDiv(height << 1, 64), 1);
      GlProgram downsampleSecondProgram = this.programs.getDownsampleSecondProgram();
      downsampleSecondProgram.bind();
      downsampleSecondProgram.setUInt("mip_levels", mipLevels);

      for (int baseMipLevel = 0; baseMipLevel + 1 < mipLevels; baseMipLevel += 6) {
         GL46.glMemoryBarrier(32);
         downsampleSecondProgram.setUInt("base_mip_level", baseMipLevel);

         for (int i = 0; i < Math.min(7, mipLevels - baseMipLevel); i++) {
            GL46.glBindImageTexture(i, this.pyramidTextureId, baseMipLevel + i, false, 0, 35001, 33326);
         }

         GL46.glDispatchCompute(MoreMath.ceilingDiv(width >> baseMipLevel, 64), MoreMath.ceilingDiv(height >> baseMipLevel, 64), 1);
      }

      GL46.glMemoryBarrier(8);
   }

   public void bindForCull() {
      GlTextureUnit.T0.makeActive();
      GlStateManager._bindTexture(this.pyramidTextureId);
   }

   public void delete() {
      if (this.pyramidTextureId != -1) {
         GL32.glDeleteTextures(this.pyramidTextureId);
         this.pyramidTextureId = -1;
      }
   }

   private void createPyramidMips(int mipLevels, int width, int height) {
      if (this.lastWidth != width || this.lastHeight != height) {
         this.lastWidth = width;
         this.lastHeight = height;
         this.delete();
         this.pyramidTextureId = GL46.glCreateTextures(3553);
         GL46.glTextureStorage2D(this.pyramidTextureId, mipLevels, 33326, width, height);
         GL46.glTextureParameteri(this.pyramidTextureId, 10241, 9728);
         GL46.glTextureParameteri(this.pyramidTextureId, 10240, 9728);
         GL46.glTextureParameteri(this.pyramidTextureId, 34892, 0);
         GL46.glTextureParameteri(this.pyramidTextureId, 10242, 33071);
         GL46.glTextureParameteri(this.pyramidTextureId, 10243, 33071);
      }
   }

   public static int mip0Size(int screenSize) {
      return Integer.highestOneBit(screenSize);
   }

   public static int getImageMipLevels(int width, int height) {
      int result = 1;

      while (width > 1 && height > 1) {
         result++;
         width >>= 1;
         height >>= 1;
      }

      return result;
   }
}
