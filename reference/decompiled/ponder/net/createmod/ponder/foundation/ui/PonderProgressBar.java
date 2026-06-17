package net.createmod.ponder.foundation.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class PonderProgressBar extends AbstractSimiWidget {
   public static final Couple<Color> BAR_COLORS = Couple.create(new Color(-2136298787, true), new Color(1353362141, true)).map(Color::setImmutable);
   LerpedFloat progress;
   PonderUI ponder;

   public PonderProgressBar(PonderUI ponder, int xIn, int yIn, int widthIn, int heightIn) {
      super(xIn, yIn, widthIn, heightIn);
      this.ponder = ponder;
      this.progress = LerpedFloat.linear().startWithValue(0.0);
   }

   @Override
   public void tick() {
      this.progress.chase((double)this.ponder.getActiveScene().getSceneProgress(), 0.5, LerpedFloat.Chaser.EXP);
      this.progress.tickChaser();
   }

   @Override
   protected boolean clicked(double mouseX, double mouseY) {
      return this.active
         && this.visible
         && this.ponder.getActiveScene().getKeyframeCount() > 0
         && mouseX >= (double)this.getX()
         && mouseX < (double)(this.getX() + this.width + 4)
         && mouseY >= (double)this.getY() - 3.0
         && mouseY < (double)(this.getY() + this.height + 20);
   }

   public void setFocused(boolean focused) {
      super.setFocused(false);
   }

   @Override
   public void onClick(double mouseX, double mouseY) {
      PonderScene activeScene = this.ponder.getActiveScene();
      int keyframeIndex = this.getHoveredKeyframeIndex(activeScene, mouseX);
      if (keyframeIndex == -1) {
         this.ponder.seekToTime(0);
      } else if (keyframeIndex == activeScene.getKeyframeCount()) {
         this.ponder.seekToTime(activeScene.getTotalTime());
      } else {
         this.ponder.seekToTime(activeScene.getKeyframeTime(keyframeIndex));
      }
   }

   public int getHoveredKeyframeIndex(PonderScene activeScene, double mouseX) {
      int totalTime = activeScene.getTotalTime();
      int clickedAtTime = (int)((mouseX - (double)this.getX()) / ((double)this.width + 4.0) * (double)totalTime);
      int lastKeyframeTime = activeScene.getKeyframeTime(activeScene.getKeyframeCount() - 1);
      int diffToEnd = totalTime - clickedAtTime;
      int diffToLast = clickedAtTime - lastKeyframeTime;
      if (diffToEnd > 0 && diffToEnd < diffToLast / 2) {
         return activeScene.getKeyframeCount();
      } else {
         lastKeyframeTime = -1;

         for (int i = 0; i < activeScene.getKeyframeCount(); lastKeyframeTime = i++) {
            diffToLast = activeScene.getKeyframeTime(i);
            if (diffToLast > clickedAtTime) {
               break;
            }
         }

         return lastKeyframeTime;
      }
   }

   @Override
   public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      PoseStack poseStack = graphics.pose();
      this.isHovered = this.clicked((double)mouseX, (double)mouseY);
      new BoxElement()
         .<BoxElement>withBackground(PonderUI.BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(PonderUI.COLOR_IDLE)
         .<RenderElement>at((float)this.getX(), (float)this.getY(), 400.0F)
         .<RenderElement>withBounds(this.width, this.height)
         .render(graphics);
      poseStack.pushPose();
      poseStack.translate((float)(this.getX() - 2), (float)(this.getY() - 2), 100.0F);
      poseStack.pushPose();
      poseStack.scale((float)(this.width + 4) * this.progress.getValue(partialTicks), 1.0F, 1.0F);
      Color c1 = BAR_COLORS.getFirst();
      Color c2 = BAR_COLORS.getSecond();
      UIRenderHelper.drawGradientRect(poseStack.last().pose(), 310, 0.0F, 1.0F, 1.0F, 3.0F, c1, c1);
      UIRenderHelper.drawGradientRect(poseStack.last().pose(), 310, 0.0F, 3.0F, 1.0F, 4.0F, c2, c2);
      poseStack.popPose();
      this.renderKeyframes(graphics, mouseX, partialTicks);
      poseStack.popPose();
   }

   private void renderKeyframes(GuiGraphics graphics, int mouseX, float partialTicks) {
      PonderScene activeScene = this.ponder.getActiveScene();
      Couple<Color> hover = PonderUI.COLOR_HOVER.map(c -> c.setAlpha(224));
      Couple<Color> idle = PonderUI.COLOR_HOVER.map(c -> c.setAlpha(112));
      int hoverIndex;
      if (this.isHovered) {
         hoverIndex = this.getHoveredKeyframeIndex(activeScene, (double)mouseX);
      } else {
         hoverIndex = -2;
      }

      if (hoverIndex == -1) {
         this.drawKeyframe(graphics, activeScene, true, 0, 0, hover.getFirst(), hover.getSecond(), 8);
      } else if (hoverIndex == activeScene.getKeyframeCount()) {
         this.drawKeyframe(graphics, activeScene, true, activeScene.getTotalTime(), this.width + 4, hover.getFirst(), hover.getSecond(), 8);
      }

      for (int i = 0; i < activeScene.getKeyframeCount(); i++) {
         int keyframeTime = activeScene.getKeyframeTime(i);
         int keyframePos = (int)((float)keyframeTime / (float)activeScene.getTotalTime() * (float)(this.width + 2));
         boolean selected = i == hoverIndex;
         Couple<Color> colors = selected ? hover : idle;
         int height = selected ? 8 : 4;
         this.drawKeyframe(graphics, activeScene, selected, keyframeTime, keyframePos, colors.getFirst(), colors.getSecond(), height);
      }
   }

   private void drawKeyframe(
      GuiGraphics graphics, PonderScene activeScene, boolean selected, int keyframeTime, int keyframePos, Color startColor, Color endColor, int height
   ) {
      PoseStack poseStack = graphics.pose();
      if (selected) {
         Font font = Minecraft.getInstance().font;
         UIRenderHelper.drawGradientRect(
            poseStack.last().pose(), 320, (float)keyframePos, 9.0F, (float)keyframePos + 2.0F, 9.0F + (float)height, endColor, startColor
         );
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.0F, 320.0F);
         String text;
         int offset;
         if (activeScene.getCurrentTime() < keyframeTime) {
            text = ">";
            offset = -2 - font.width(text);
         } else {
            text = "<";
            offset = 4;
         }

         graphics.drawString(font, Component.literal(text).withStyle(ChatFormatting.BOLD), keyframePos + offset, 10, endColor.getRGB(), false);
         poseStack.popPose();
      }

      UIRenderHelper.drawGradientRect(
         poseStack.last().pose(), 320, (float)keyframePos, 0.0F, (float)keyframePos + 2.0F, 1.0F + (float)height, startColor, endColor
      );
   }

   public void playDownSound(SoundManager handler) {
   }
}
