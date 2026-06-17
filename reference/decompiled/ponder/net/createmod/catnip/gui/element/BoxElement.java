package net.createmod.catnip.gui.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

public class BoxElement extends AbstractRenderElement {
   public static final Couple<Color> COLOR_VANILLA_BORDER = Couple.create(new Color(1347420415, true), new Color(1344798847, true)).map(Color::setImmutable);
   public static final Color COLOR_VANILLA_BACKGROUND = new Color(-267386864, true).setImmutable();
   public static final Color COLOR_BACKGROUND_FLAT = new Color(-16777216, true).setImmutable();
   public static final Color COLOR_BACKGROUND_TRANSPARENT = new Color(-587202560, true).setImmutable();
   protected Color background = COLOR_VANILLA_BACKGROUND;
   protected Color borderTop = COLOR_VANILLA_BORDER.getFirst();
   protected Color borderBot = COLOR_VANILLA_BORDER.getSecond();
   protected int borderOffset = 2;

   public <T extends BoxElement> T withBackground(Color color) {
      this.background = color;
      return (T)this;
   }

   public <T extends BoxElement> T withBackground(int color) {
      return this.withBackground(new Color(color, true));
   }

   public <T extends BoxElement> T flatBorder(Color color) {
      this.borderTop = color;
      this.borderBot = color;
      return (T)this;
   }

   public <T extends BoxElement> T flatBorder(int color) {
      return this.flatBorder(new Color(color, true));
   }

   public <T extends BoxElement> T gradientBorder(Couple<Color> colors) {
      this.borderTop = colors.getFirst();
      this.borderBot = colors.getSecond();
      return (T)this;
   }

   public <T extends BoxElement> T gradientBorder(Color top, Color bot) {
      this.borderTop = top;
      this.borderBot = bot;
      return (T)this;
   }

   public <T extends BoxElement> T gradientBorder(int top, int bot) {
      return this.gradientBorder(new Color(top, true), new Color(bot, true));
   }

   public <T extends BoxElement> T withBorderOffset(int offset) {
      this.borderOffset = offset;
      return (T)this;
   }

   @Override
   public void render(GuiGraphics graphics) {
      this.renderBox(graphics);
   }

   protected void renderBox(GuiGraphics graphics) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      PoseStack ms = graphics.pose();
      Matrix4f model = ms.last().pose();
      int f = this.borderOffset;
      Color c1 = this.background.copy().scaleAlpha(this.alpha);
      Color c2 = this.borderTop.copy().scaleAlpha(this.alpha);
      Color c3 = this.borderBot.copy().scaleAlpha(this.alpha);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder b = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      b.addVertex(model, this.x - (float)f - 1.0F, this.y - (float)f - 2.0F, this.z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y - (float)f - 1.0F, this.z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y - (float)f - 1.0F, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y - (float)f - 2.0F, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 2.0F, this.y - (float)f - 1.0F, this.z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 2.0F, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y - (float)f - 1.0F, this.z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y + (float)f + 2.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y + (float)f + 2.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y - (float)f - 1.0F, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 2.0F + (float)this.width, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 2.0F + (float)this.width, this.y - (float)f - 1.0F, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y - (float)f - 1.0F, this.z).setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y - (float)f - 1.0F, this.z)
         .setColor(c1.getRed(), c1.getGreen(), c1.getBlue(), c1.getAlpha());
      BufferUploader.drawWithShader(b.buildOrThrow());
      b = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      b.addVertex(model, this.x - (float)f - 1.0F, this.y - (float)f - 1.0F, this.z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y - (float)f, this.z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y - (float)f, this.z)
         .setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y - (float)f - 1.0F, this.z)
         .setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y - (float)f, this.z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y + (float)f + (float)this.height, this.z)
         .setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
      b.addVertex(model, this.x - (float)f, this.y + (float)f + (float)this.height, this.z).setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
      b.addVertex(model, this.x - (float)f, this.y - (float)f, this.z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y + (float)f + (float)this.height, this.z)
         .setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
      b.addVertex(model, this.x - (float)f - 1.0F, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y + (float)f + 1.0F + (float)this.height, this.z)
         .setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y + (float)f + (float)this.height, this.z)
         .setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
      b.addVertex(model, this.x + (float)f + (float)this.width, this.y - (float)f, this.z).setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
      b.addVertex(model, this.x + (float)f + (float)this.width, this.y + (float)f + (float)this.height, this.z)
         .setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y + (float)f + (float)this.height, this.z)
         .setColor(c3.getRed(), c3.getGreen(), c3.getBlue(), c3.getAlpha());
      b.addVertex(model, this.x + (float)f + 1.0F + (float)this.width, this.y - (float)f, this.z)
         .setColor(c2.getRed(), c2.getGreen(), c2.getBlue(), c2.getAlpha());
      BufferUploader.drawWithShader(b.buildOrThrow());
      RenderSystem.disableBlend();
   }
}
