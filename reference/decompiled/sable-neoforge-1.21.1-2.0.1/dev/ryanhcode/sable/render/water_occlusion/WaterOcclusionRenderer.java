package dev.ryanhcode.sable.render.water_occlusion;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegion;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11C;

@Internal
public class WaterOcclusionRenderer {
   private final Set<SimpleCulledRenderRegion> regions = new ObjectOpenHashSet();
   private AdvancedFbo closeBuffer;
   private AdvancedFbo farBuffer;
   private Level level;
   private static boolean isEnabled = false;

   public static boolean isEnabled() {
      return isEnabled;
   }

   public static void setIsEnabled(boolean isEnabled) {
      WaterOcclusionRenderer.isEnabled = isEnabled;
   }

   @Nullable
   @Internal
   public SimpleCulledRenderRegion addRegion(Collection<BlockPos> blocks) {
      if (blocks.isEmpty()) {
         return null;
      } else {
         SimpleCulledRenderRegion region = new WaterOcclusionRenderRegion(blocks);
         this.regions.add(region);
         return region;
      }
   }

   public void removeRegion(SimpleCulledRenderRegion region) {
      region.free();
      this.regions.remove(region);
   }

   private void updateFramebuffers(boolean needed) {
      Minecraft minecraft = Minecraft.getInstance();
      RenderTarget renderTarget = minecraft.getMainRenderTarget();
      if (!needed && this.closeBuffer != null) {
         this.closeBuffer.free();
         this.farBuffer.free();
         this.closeBuffer = null;
         this.farBuffer = null;
      }

      if (needed && (this.closeBuffer == null || renderTarget.width != this.closeBuffer.getWidth() || renderTarget.height != this.farBuffer.getHeight())) {
         if (this.closeBuffer != null) {
            this.closeBuffer.free();
            this.farBuffer.free();
         }

         this.closeBuffer = AdvancedFbo.withSize(renderTarget.width, renderTarget.height).addColorTextureBuffer().setDepthTextureBuffer().build(true);
         this.farBuffer = AdvancedFbo.withSize(renderTarget.width, renderTarget.height).addColorTextureBuffer().setDepthTextureBuffer().build(true);
      }
   }

   public void preRenderTranslucent(Matrix4f modelView, Matrix4f projMat) {
      if (isEnabled()) {
         WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(this.level);
         boolean needed = !this.regions.isEmpty() || container == null;
         this.updateFramebuffers(needed);
         if (needed) {
            Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            this.closeBuffer.bind(true);
            boolean cameraOccluded = container.isOccluded(cameraPos);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            GL11C.glEnable(34383);
            this.closeBuffer.clear(0.0F, 0.0F, 0.0F, 0.0F, cameraOccluded ? 0.0F : 1.0F, this.closeBuffer.getClearMask());
            GL11C.glCullFace(1029);

            for (SimpleCulledRenderRegion region : this.regions) {
               region.render(modelView, projMat);
            }

            AdvancedFbo.unbind();
            this.farBuffer.bind(true);
            this.farBuffer.clear();
            GL11C.glCullFace(1028);

            for (SimpleCulledRenderRegion region : this.regions) {
               region.render(modelView, projMat);
            }

            AdvancedFbo.unbind();
            GL11C.glCullFace(1029);
            GL11C.glDisable(34383);
            ShaderProgram.unbind();
         }
      }
   }

   public void setupTranslucentShader(ShaderInstance shader) {
      if (isEnabled()) {
         Uniform uniform = shader.getUniform("SableWaterOcclusionEnabled");
         if (this.closeBuffer == null) {
            if (uniform != null) {
               uniform.set(0.0F);
            }
         } else {
            Window window = Minecraft.getInstance().getWindow();
            Uniform screenSize = shader.getUniform("ScreenSize");
            if (screenSize != null) {
               screenSize.set((float)window.getWidth(), (float)window.getHeight());
            }

            if (uniform != null) {
               uniform.set(1.0F);
            }

            shader.setSampler("SableCloseSampler", this.closeBuffer.getDepthTextureAttachment());
            shader.setSampler("SableFarSampler", this.farBuffer.getDepthTextureAttachment());
         }
      }
   }

   public void update() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level != this.level) {
         this.level = minecraft.level;
         this.regions.forEach(SimpleCulledRenderRegion::free);
         this.regions.clear();
      }
   }
}
