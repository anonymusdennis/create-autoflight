package dev.simulated_team.simulated.content.end_sea;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.simulated_team.simulated.Simulated;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import foundry.veil.api.client.render.shader.uniform.ShaderUniform;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3dc;

public class EndSeaRenderer {
   private static final ResourceLocation SHADER = Simulated.path("end_sea");
   private static final int LAYER_COUNT = 48;
   private static final VertexFormat FORMAT = VertexFormat.builder()
      .add("Position", VertexFormatElement.POSITION)
      .add("Color", VertexFormatElement.COLOR)
      .add("UV0", VertexFormatElement.UV0)
      .add("UV2", VertexFormatElement.UV2)
      .build();
   private static final Vec3[] LAYER_COLORS = new Vec3[]{
      new Vec3(0.022087, 0.098399, 0.110818),
      new Vec3(0.011892, 0.095924, 0.089485),
      new Vec3(0.027636, 0.101689, 0.100326),
      new Vec3(0.046564, 0.109883, 0.114838),
      new Vec3(0.064901, 0.117696, 0.097189),
      new Vec3(0.063761, 0.086895, 0.123646),
      new Vec3(0.084817, 0.111994, 0.16638),
      new Vec3(0.097489, 0.15412, 0.091064),
      new Vec3(0.106152, 0.131144, 0.195191),
      new Vec3(0.097721, 0.110188, 0.187229),
      new Vec3(0.133516, 0.138278, 0.148582),
      new Vec3(0.070006, 0.243332, 0.235792),
      new Vec3(0.196766, 0.142899, 0.214696),
      new Vec3(0.047281, 0.315338, 0.32197),
      new Vec3(0.204675, 0.39001, 0.302066),
      new Vec3(0.080955, 0.314821, 0.661491)
   };

   public static void tick() {
   }

   public static void render(Camera camera, GameRenderer gameRenderer) {
      Minecraft minecraft = Minecraft.getInstance();
      EndSeaPhysics physics = EndSeaPhysicsData.of(minecraft.level);
      if (physics != null) {
         if (!EndSeaShadowRenderer.renderingShadowMap()) {
            renderLayers(physics, camera);
         }
      }
   }

   private static void renderLayers(EndSeaPhysics physics, Camera camera) {
      Minecraft minecraft = Minecraft.getInstance();
      ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
      shader.bind();
      shader.setDefaultUniforms(Mode.QUADS);
      AdvancedFbo shadowBuffer = EndSeaShadowRenderer.getShadowsFramebuffer();
      shader.setTexture("ShadowDepthSampler", 3553, shadowBuffer.getDepthTextureAttachment().getId());
      shader.setTexture("ShadowStrengthSampler", 3553, shadowBuffer.getColorTextureAttachment(0).getId());
      BufferBuilder builder = Tesselator.getInstance().begin(Mode.QUADS, FORMAT);

      for (int i = 0; i < LAYER_COLORS.length; i++) {
         LAYER_COLORS[i] = LAYER_COLORS[i].lerp(LAYER_COLORS[i].normalize(), 1.0);
      }

      Vector3dc renderOrigin = EndSeaShadowRenderer.getLastRenderOrigin();
      PoseStack poseStack = new PoseStack();
      poseStack.translate(renderOrigin.x() - camera.getPosition().x, 0.0, renderOrigin.z() - camera.getPosition().z);
      poseStack.scale(128.0F, 1.0F, 128.0F);
      poseStack.translate(0.0, physics.startY() - camera.getPosition().y, 0.0);
      ShaderUniform volumeSize = shader.getUniform("ShadowVolumeSize");
      if (volumeSize != null) {
         volumeSize.setFloat(128.0F);
      }

      ShaderUniform startY = shader.getUniform("StartY");
      if (startY != null) {
         startY.setFloat((float)physics.startY());
      }

      LocalPlayer player = minecraft.player;
      float renderTime = (float)player.tickCount + minecraft.getTimer().getGameTimeDeltaPartialTick(false);

      for (int i = 0; i < 48; i++) {
         Vec3 layer = LAYER_COLORS[i % LAYER_COLORS.length];
         float yCoord = (float)(-i) / 2.0F;
         float uvScale = 10.24F;
         float uvShift = (float)Mth.frac((double)renderTime / 2000.0) + (float)(renderOrigin.z() / 128.0 * 10.24F / 2.0);
         float parallelUVShift = (float)((double)((float)(layer.x + layer.y)) + renderOrigin.x() / 128.0 * 10.24F / 2.0);
         float alpha = 1.0F;
         Matrix4f pose = poseStack.last().pose();
         builder.addVertex(pose, -1.0F, yCoord, -1.0F)
            .setColor((float)layer.x, (float)layer.y, (float)layer.z, 1.0F)
            .setUv(0.0F + parallelUVShift, 0.0F + uvShift)
            .setUv2(0, 0);
         builder.addVertex(pose, 1.0F, yCoord, -1.0F)
            .setColor((float)layer.x, (float)layer.y, (float)layer.z, 1.0F)
            .setUv(10.24F + parallelUVShift, 0.0F + uvShift)
            .setUv2(1, 0);
         builder.addVertex(pose, 1.0F, yCoord, 1.0F)
            .setColor((float)layer.x, (float)layer.y, (float)layer.z, 1.0F)
            .setUv(10.24F + parallelUVShift, 10.24F + uvShift)
            .setUv2(1, 1);
         builder.addVertex(pose, -1.0F, yCoord, 1.0F)
            .setColor((float)layer.x, (float)layer.y, (float)layer.z, 1.0F)
            .setUv(0.0F + parallelUVShift, 10.24F + uvShift)
            .setUv2(0, 1);
      }

      RenderSystem.disableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ONE);
      BufferUploader.drawWithShader(builder.buildOrThrow());
      ShaderProgram.unbind();
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.depthMask(true);
   }
}
