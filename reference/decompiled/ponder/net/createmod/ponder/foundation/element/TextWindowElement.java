package net.createmod.ponder.foundation.element;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.TextElementBuilder;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class TextWindowElement extends AnimatedOverlayElementBase {
   public static final Couple<Color> COLOR_WINDOW_BORDER = Couple.create(new Color(1618632704, true), new Color(544890880, true)).map(Color::setImmutable);
   Supplier<String> textGetter = () -> "(?) No text was provided";
   @Nullable
   String bakedText;
   int y;
   @Nullable
   Vec3 vec;
   boolean nearScene = false;
   PonderPalette palette = PonderPalette.WHITE;

   public TextElementBuilder builder(PonderScene scene) {
      return new TextWindowElement.Builder(scene);
   }

   @Override
   public void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks, float fade) {
      if (this.bakedText == null) {
         this.bakedText = this.textGetter.get();
      }

      if (!(fade < 0.0625F)) {
         PonderScene.SceneTransform transform = scene.getTransform();
         Vec2 sceneToScreen = this.vec != null
            ? transform.sceneToScreen(this.vec, partialTicks)
            : new Vec2((float)screen.width / 2.0F, (float)(screen.height - 200) / 2.0F + (float)this.y - 8.0F);
         boolean settled = transform.xRotation.settled() && transform.yRotation.settled();
         float pY = settled ? (float)((int)sceneToScreen.y) : sceneToScreen.y;
         float yDiff = ((float)screen.height / 2.0F - sceneToScreen.y - 10.0F) / 100.0F;
         float targetX = (float)screen.width * Mth.lerp(yDiff * yDiff, 0.75F, 0.625F);
         if (this.nearScene) {
            targetX = Math.min(targetX, sceneToScreen.x + 50.0F);
         }

         if (settled) {
            targetX = (float)((int)targetX);
         }

         int textWidth = (int)Math.min((float)screen.width - targetX, 180.0F);
         List<FormattedText> lines = screen.getFontRenderer().getSplitter().splitLines(this.bakedText, textWidth, Style.EMPTY);
         int boxWidth = 0;

         for (FormattedText line : lines) {
            boxWidth = Math.max(boxWidth, screen.getFontRenderer().width(line));
         }

         int boxHeight = screen.getFontRenderer().wordWrapHeight(this.bakedText, boxWidth);
         PoseStack poseStack = graphics.pose();
         poseStack.pushPose();
         poseStack.translate(0.0F, pY, 400.0F);
         new BoxElement()
            .<BoxElement>withBackground(PonderUI.BACKGROUND_FLAT)
            .<BoxElement>gradientBorder(COLOR_WINDOW_BORDER)
            .<RenderElement>at(targetX - 10.0F, 3.0F, -101.0F)
            .<RenderElement>withBounds(boxWidth, boxHeight - 1)
            .render(graphics);
         Color brighter = this.palette.getColorObject().mixWith(new Color(-35), 0.5F).setImmutable();
         Color c1 = new Color(-11974327);
         Color c2 = new Color(-13027015);
         if (this.vec != null) {
            poseStack.pushPose();
            poseStack.translate(sceneToScreen.x, 0.0F, 0.0F);
            double lineTarget = (double)((targetX - sceneToScreen.x) * fade);
            poseStack.scale((float)lineTarget, 1.0F, 1.0F);
            graphics.fillGradient(0, 0, 1, 1, -100, brighter.getRGB(), brighter.getRGB());
            graphics.fillGradient(0, 1, 1, 2, -100, c1.getRGB(), c2.getRGB());
            poseStack.popPose();
         }

         poseStack.translate(0.0F, 0.0F, 400.0F);

         for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(
               screen.getFontRenderer(), lines.get(i).getString(), (int)(targetX - 10.0F), 3 + 9 * i, brighter.scaleAlphaForText(fade).getRGB(), false
            );
         }

         poseStack.popPose();
      }
   }

   public PonderPalette getPalette() {
      return this.palette;
   }

   private class Builder implements TextElementBuilder {
      private final PonderScene scene;

      public Builder(PonderScene scene) {
         this.scene = scene;
      }

      public TextWindowElement.Builder colored(PonderPalette color) {
         TextWindowElement.this.palette = color;
         return this;
      }

      public TextWindowElement.Builder pointAt(Vec3 vec) {
         TextWindowElement.this.vec = vec;
         return this;
      }

      public TextWindowElement.Builder independent(int y) {
         TextWindowElement.this.y = y;
         return this;
      }

      public TextWindowElement.Builder text(String defaultText) {
         TextWindowElement.this.textGetter = this.scene.registerText(defaultText);
         return this;
      }

      @Override
      public TextElementBuilder text(String defaultText, Object... params) {
         TextWindowElement.this.textGetter = this.scene.registerText(defaultText, params);
         return this;
      }

      public TextWindowElement.Builder sharedText(ResourceLocation key) {
         TextWindowElement.this.textGetter = () -> PonderIndex.getLangAccess().getShared(key);
         return this;
      }

      @Override
      public TextElementBuilder sharedText(ResourceLocation key, Object... params) {
         TextWindowElement.this.textGetter = () -> PonderIndex.getLangAccess().getShared(key, params);
         return this;
      }

      public TextWindowElement.Builder sharedText(String key) {
         return this.sharedText(ResourceLocation.fromNamespaceAndPath(this.scene.getNamespace(), key));
      }

      @Override
      public TextElementBuilder sharedText(String key, Object... params) {
         return this.sharedText(ResourceLocation.fromNamespaceAndPath(this.scene.getNamespace(), key), params);
      }

      public TextWindowElement.Builder placeNearTarget() {
         TextWindowElement.this.nearScene = true;
         return this;
      }

      public TextWindowElement.Builder attachKeyFrame() {
         this.scene.builder().addLazyKeyframe();
         return this;
      }
   }
}
