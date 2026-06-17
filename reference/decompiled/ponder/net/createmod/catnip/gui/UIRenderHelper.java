package net.createmod.catnip.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Float;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.platform.CatnipClientServices;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class UIRenderHelper {
   public static final Couple<Color> COLOR_TEXT = Couple.create(new Color(-1118482), new Color(-6052957)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_TEXT_DARKER = Couple.create(new Color(-6052957), new Color(-8355712)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_TEXT_ACCENT = Couple.create(new Color(-2232577), new Color(-6246208)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_TEXT_STRONG_ACCENT = Couple.create(new Color(-7686442), new Color(-9530709)).map(Color::setImmutable);
   public static final Color COLOR_STREAK = new Color(1052688, false).setImmutable();
   @Nullable
   public static UIRenderHelper.CustomRenderTarget framebuffer;

   public static void init() {
      RenderSystem.recordRenderCall(() -> {
         Window mainWindow = Minecraft.getInstance().getWindow();
         framebuffer = UIRenderHelper.CustomRenderTarget.create(mainWindow);
      });
   }

   public static void updateWindowSize(Window mainWindow) {
      if (framebuffer != null) {
         framebuffer.resize(mainWindow.getWidth(), mainWindow.getHeight(), Minecraft.ON_OSX);
      }
   }

   public static void drawFramebuffer(PoseStack poseStack, float alpha) {
      if (framebuffer != null) {
         framebuffer.renderWithAlpha(poseStack, alpha);
      }
   }

   public static void swapAndBlitColor(RenderTarget src, RenderTarget dst) {
      GlStateManager._glBindFramebuffer(36008, src.frameBufferId);
      GlStateManager._glBindFramebuffer(36009, dst.frameBufferId);
      GlStateManager._glBlitFrameBuffer(0, 0, src.viewWidth, src.viewHeight, 0, 0, dst.viewWidth, dst.viewHeight, 16384, 9729);
      GlStateManager._glBindFramebuffer(36160, dst.frameBufferId);
   }

   public static void streak(GuiGraphics graphics, float angle, int x, int y, int breadth, int length) {
      streak(graphics, angle, x, y, breadth, length, COLOR_STREAK);
   }

   public static void streak(GuiGraphics graphics, float angle, int x, int y, int breadth, int length, Color c) {
      Color color = c.copy().setImmutable();
      Color c1 = color.scaleAlpha(0.625F);
      Color c2 = color.scaleAlpha(0.5F);
      Color c3 = color.scaleAlpha(0.0625F);
      Color c4 = color.scaleAlpha(0.0F);
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate((float)x, (float)y, 0.0F);
      poseStack.mulPose(Axis.ZP.rotationDegrees(angle - 90.0F));
      streak(graphics, breadth / 2, length, c1, c2, c3, c4);
      poseStack.popPose();
   }

   private static void streak(GuiGraphics graphics, int width, int height, Color c1, Color c2, Color c3, Color c4) {
      if (!NavigatableSimiScreen.isCurrentlyRenderingPreviousScreen()) {
         double split1 = 0.5;
         double split2 = 0.75;
         graphics.fillGradient(-width, 0, width, (int)(split1 * (double)height), c1.getRGB(), c2.getRGB());
         graphics.fillGradient(-width, (int)(split1 * (double)height), width, (int)(split2 * (double)height), c2.getRGB(), c3.getRGB());
         graphics.fillGradient(-width, (int)(split2 * (double)height), width, height, c3.getRGB(), c4.getRGB());
      }
   }

   public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, float breadth, float length, Couple<Color> c) {
      angledGradient(graphics, angle, x, y, 0, breadth, length, c);
   }

   public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, int z, float breadth, float length, Couple<Color> c) {
      angledGradient(graphics, angle, x, y, z, breadth, length, c.getFirst(), c.getSecond());
   }

   public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, float breadth, float length, Color color1, Color color2) {
      angledGradient(graphics, angle, x, y, 0, breadth, length, color1, color2);
   }

   public static void angledGradient(GuiGraphics graphics, float angle, int x, int y, int z, float breadth, float length, Color startColor, Color endColor) {
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate((float)x, (float)y, (float)z);
      poseStack.mulPose(Axis.ZP.rotationDegrees(angle - 90.0F));
      float w = breadth / 2.0F;
      drawGradientRect(poseStack.last().pose(), 0, -w, 0.0F, w, length, startColor, endColor);
      poseStack.popPose();
   }

   public static void drawGradientRect(Matrix4f mat, int zLevel, float left, float top, float right, float bottom, Color startColor, Color endColor) {
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      buffer.addVertex(mat, right, top, (float)zLevel).setColor(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha());
      buffer.addVertex(mat, left, top, (float)zLevel).setColor(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), startColor.getAlpha());
      buffer.addVertex(mat, left, bottom, (float)zLevel).setColor(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), endColor.getAlpha());
      buffer.addVertex(mat, right, bottom, (float)zLevel).setColor(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), endColor.getAlpha());
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      RenderSystem.disableBlend();
   }

   public static void breadcrumbArrow(GuiGraphics graphics, int x, int y, int z, int width, int height, int indent, Couple<Color> colors) {
      breadcrumbArrow(graphics, x, y, z, width, height, indent, colors.getFirst(), colors.getSecond());
   }

   public static void breadcrumbArrow(GuiGraphics graphics, int x, int y, int z, int width, int height, int indent, Color startColor, Color endColor) {
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate((float)(x - indent), (float)y, (float)z);
      breadcrumbArrow(graphics, width, height, indent, startColor, endColor);
      poseStack.popPose();
   }

   private static void breadcrumbArrow(GuiGraphics graphics, int width, int height, int indent, Color c1, Color c2) {
      float x0 = 0.0F;
      float x1 = (float)indent;
      float x2 = (float)width;
      float x3 = (float)(indent + width);
      float y0 = 0.0F;
      float y1 = (float)height / 2.0F;
      float y2 = (float)height;
      indent = Math.abs(indent);
      width = Math.abs(width);
      Color fc1 = Color.mixColors(c1, c2, 0.0F);
      Color fc2 = Color.mixColors(c1, c2, (float)indent / ((float)width + 2.0F * (float)indent));
      Color fc3 = Color.mixColors(c1, c2, (float)(indent + width) / ((float)width + 2.0F * (float)indent));
      Color fc4 = Color.mixColors(c1, c2, 1.0F);
      RenderSystem.disableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.disableCull();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tessellator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tessellator.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f model = graphics.pose().last().pose();
      bufferbuilder.addVertex(model, x0, y1, 0.0F).setColor(fc1.getRed(), fc1.getGreen(), fc1.getBlue(), fc1.getAlpha());
      bufferbuilder.addVertex(model, x1, y0, 0.0F).setColor(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha());
      bufferbuilder.addVertex(model, x1, y1, 0.0F).setColor(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha());
      bufferbuilder.addVertex(model, x0, y1, 0.0F).setColor(fc1.getRed(), fc1.getGreen(), fc1.getBlue(), fc1.getAlpha());
      bufferbuilder.addVertex(model, x1, y1, 0.0F).setColor(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha());
      bufferbuilder.addVertex(model, x1, y2, 0.0F).setColor(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha());
      bufferbuilder.addVertex(model, x1, y2, 0.0F).setColor(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha());
      bufferbuilder.addVertex(model, x1, y0, 0.0F).setColor(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha());
      bufferbuilder.addVertex(model, x2, y0, 0.0F).setColor(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha());
      bufferbuilder.addVertex(model, x1, y2, 0.0F).setColor(fc2.getRed(), fc2.getGreen(), fc2.getBlue(), fc2.getAlpha());
      bufferbuilder.addVertex(model, x2, y0, 0.0F).setColor(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha());
      bufferbuilder.addVertex(model, x2, y2, 0.0F).setColor(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha());
      bufferbuilder.addVertex(model, x2, y1, 0.0F).setColor(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha());
      bufferbuilder.addVertex(model, x2, y0, 0.0F).setColor(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha());
      bufferbuilder.addVertex(model, x3, y0, 0.0F).setColor(fc4.getRed(), fc4.getGreen(), fc4.getBlue(), fc4.getAlpha());
      bufferbuilder.addVertex(model, x2, y2, 0.0F).setColor(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha());
      bufferbuilder.addVertex(model, x2, y1, 0.0F).setColor(fc3.getRed(), fc3.getGreen(), fc3.getBlue(), fc3.getAlpha());
      bufferbuilder.addVertex(model, x3, y2, 0.0F).setColor(fc4.getRed(), fc4.getGreen(), fc4.getBlue(), fc4.getAlpha());
      BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   public static void drawRadialSector(
      GuiGraphics graphics, float innerRadius, float outerRadius, float startAngle, float arcAngle, Color innerColor, Color outerColor
   ) {
      List<Point2D> innerPoints = getPointsForCircleArc(innerRadius, startAngle, arcAngle);
      List<Point2D> outerPoints = getPointsForCircleArc(outerRadius, startAngle, arcAngle);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      BufferBuilder builder = Tesselator.getInstance().begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f pose = graphics.pose().last().pose();
      Matrix3f n = graphics.pose().last().normal();

      for (int i = 0; i < innerPoints.size(); i++) {
         Point2D point = outerPoints.get(i);
         builder.addVertex(pose, (float)point.getX(), (float)point.getY(), 0.0F).setColor(outerColor.getRGB());
         point = innerPoints.get(i);
         builder.addVertex(pose, (float)point.getX(), (float)point.getY(), 0.0F).setColor(innerColor.getRGB());
      }

      BufferUploader.drawWithShader(builder.buildOrThrow());
      RenderSystem.disableBlend();
   }

   private static List<Point2D> getPointsForCircleArc(float radius, float startAngle, float arcAngle) {
      int segmentCount = Math.abs(arcAngle) <= 90.0F ? 16 : 32;
      List<Point2D> points = new ArrayList<>(segmentCount);
      float theta = (float) (Math.PI / 180.0) * arcAngle / (float)(segmentCount - 1);
      float t = (float) (Math.PI / 180.0) * startAngle;

      for (int i = 0; i < segmentCount; i++) {
         points.add(new Float((float)((double)radius * Math.cos((double)t)), (float)((double)radius * Math.sin((double)t))));
         t += theta;
      }

      return points;
   }

   public static void drawColoredTexture(GuiGraphics graphics, Color c, int x, int y, int tex_left, int tex_top, int width, int height) {
      drawColoredTexture(graphics, c, x, y, 0, (float)tex_left, (float)tex_top, width, height, 256, 256);
   }

   public static void drawColoredTexture(
      GuiGraphics graphics, Color c, int x, int y, int z, float tex_left, float tex_top, int width, int height, int sheet_width, int sheet_height
   ) {
      drawColoredTexture(graphics, c, x, x + width, y, y + height, z, width, height, tex_left, tex_top, sheet_width, sheet_height);
   }

   public static void drawStretched(GuiGraphics graphics, int left, int top, int w, int h, int z, TextureSheetSegment tex) {
      tex.bind();
      drawTexturedQuad(
         graphics.pose().last().pose(),
         Color.WHITE,
         left,
         left + w,
         top,
         top + h,
         z,
         (float)tex.getStartX() / 256.0F,
         (float)(tex.getStartX() + tex.getWidth()) / 256.0F,
         (float)tex.getStartY() / 256.0F,
         (float)(tex.getStartY() + tex.getHeight()) / 256.0F
      );
   }

   public static void drawCropped(GuiGraphics graphics, int left, int top, int w, int h, int z, TextureSheetSegment tex) {
      tex.bind();
      drawTexturedQuad(
         graphics.pose().last().pose(),
         Color.WHITE,
         left,
         left + w,
         top,
         top + h,
         z,
         (float)tex.getStartX() / 256.0F,
         (float)(tex.getStartX() + w) / 256.0F,
         (float)tex.getStartY() / 256.0F,
         (float)(tex.getStartY() + h) / 256.0F
      );
   }

   private static void drawColoredTexture(
      GuiGraphics graphics,
      Color c,
      int left,
      int right,
      int top,
      int bot,
      int z,
      int tex_width,
      int tex_height,
      float tex_left,
      float tex_top,
      int sheet_width,
      int sheet_height
   ) {
      drawTexturedQuad(
         graphics.pose().last().pose(),
         c,
         left,
         right,
         top,
         bot,
         z,
         (tex_left + 0.0F) / (float)sheet_width,
         (tex_left + (float)tex_width) / (float)sheet_width,
         (tex_top + 0.0F) / (float)sheet_height,
         (tex_top + (float)tex_height) / (float)sheet_height
      );
   }

   private static void drawTexturedQuad(Matrix4f m, Color c, int left, int right, int top, int bot, int z, float u1, float u2, float v1, float v2) {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      bufferbuilder.addVertex(m, (float)left, (float)bot, (float)z).setColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).setUv(u1, v2);
      bufferbuilder.addVertex(m, (float)right, (float)bot, (float)z).setColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).setUv(u2, v2);
      bufferbuilder.addVertex(m, (float)right, (float)top, (float)z).setColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).setUv(u2, v1);
      bufferbuilder.addVertex(m, (float)left, (float)top, (float)z).setColor(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()).setUv(u1, v1);
      BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
      RenderSystem.disableBlend();
   }

   public static void flipForGuiRender(PoseStack poseStack) {
      poseStack.mulPose(new Matrix4f().scaling(1.0F, -1.0F, 1.0F));
   }

   public static class CustomRenderTarget extends RenderTarget {
      public CustomRenderTarget(boolean useDepth) {
         super(useDepth);
      }

      public static UIRenderHelper.CustomRenderTarget create(Window mainWindow) {
         UIRenderHelper.CustomRenderTarget framebuffer = new UIRenderHelper.CustomRenderTarget(true);
         framebuffer.resize(mainWindow.getWidth(), mainWindow.getHeight(), Minecraft.ON_OSX);
         framebuffer.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
         CatnipClientServices.CLIENT_HOOKS.enableStencilBuffer(framebuffer);
         return framebuffer;
      }

      public void renderWithAlpha(PoseStack poseStack, float alpha) {
         Window window = Minecraft.getInstance().getWindow();
         float guiScaledWidth = (float)window.getGuiScaledWidth();
         float guiScaledHeight = (float)window.getGuiScaledHeight();
         float tx = (float)this.viewWidth / (float)this.width;
         float ty = (float)this.viewHeight / (float)this.height;
         RenderSystem.disableDepthTest();
         Minecraft minecraft = Minecraft.getInstance();
         ShaderInstance shaderinstance = minecraft.gameRenderer.blitShader;
         shaderinstance.setSampler("DiffuseSampler", this.colorTextureId);
         Matrix4f matrix4f = poseStack.last().pose();
         Matrix4f projectionMatrix = RenderSystem.getProjectionMatrix();
         RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z);
         if (shaderinstance.MODEL_VIEW_MATRIX != null) {
            shaderinstance.MODEL_VIEW_MATRIX.set(new Matrix4f().translation(0.0F, 0.0F, -2000.0F));
         }

         if (shaderinstance.PROJECTION_MATRIX != null) {
            shaderinstance.PROJECTION_MATRIX.set(matrix4f);
         }

         shaderinstance.apply();
         Tesselator tesselator = RenderSystem.renderThreadTesselator();
         BufferBuilder bufferbuilder = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         bufferbuilder.addVertex(0.0F, guiScaledHeight, 0.0F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
         bufferbuilder.addVertex(guiScaledWidth, guiScaledHeight, 0.0F).setUv(tx, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha);
         bufferbuilder.addVertex(guiScaledWidth, 0.0F, 0.0F).setUv(tx, ty).setColor(1.0F, 1.0F, 1.0F, alpha);
         bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setUv(0.0F, ty).setColor(1.0F, 1.0F, 1.0F, alpha);
         BufferUploader.draw(bufferbuilder.buildOrThrow());
         shaderinstance.clear();
         RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);
      }
   }
}
