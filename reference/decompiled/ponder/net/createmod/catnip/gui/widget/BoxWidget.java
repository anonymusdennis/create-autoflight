package net.createmod.catnip.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.FadableScreenElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;

public class BoxWidget extends ElementWidget {
   public static final Function<BoxWidget, FadableScreenElement> gradientFactory = box -> (ms, w, h, alpha) -> UIRenderHelper.angledGradient(
            ms, 90.0F, w / 2, -2, (float)(w + 4), (float)(h + 4), box.gradientColor
         );
   protected BoxElement box;
   @Nullable
   protected Couple<Color> customBorder;
   @Nullable
   protected Color customBackground;
   protected Couple<Color> colorIdle = AbstractSimiWidget.COLOR_IDLE;
   protected Couple<Color> colorHover = AbstractSimiWidget.COLOR_HOVER;
   protected Couple<Color> colorClick = AbstractSimiWidget.COLOR_CLICK;
   protected Couple<Color> colorDisabled = AbstractSimiWidget.COLOR_DISABLED;
   protected boolean animateColors = true;
   protected LerpedFloat colorAnimation = LerpedFloat.linear();
   protected Couple<Color> gradientColor;
   private Couple<Color> previousGradient;
   private Couple<Color> gradientTarget;

   public BoxWidget() {
      this(0, 0);
   }

   public BoxWidget(int x, int y) {
      this(x, y, 16, 16);
   }

   public BoxWidget(int x, int y, int width, int height) {
      super(x, y, width, height);
      this.box = new BoxElement().<RenderElement>at((float)x, (float)y).withBounds(width, height);
      this.previousGradient = this.gradientColor = this.gradientTarget = this.getColorIdle();
   }

   public <T extends BoxWidget> T withBounds(int width, int height) {
      this.width = width;
      this.height = height;
      return (T)this;
   }

   public <T extends BoxWidget> T withBorderColors(Couple<Color> colors) {
      this.customBorder = colors;
      this.updateGradientFromState();
      return (T)this;
   }

   public <T extends BoxWidget> T withBorderColors(Color top, Color bot) {
      return this.withBorderColors(Couple.create(top, bot));
   }

   public <T extends BoxWidget> T withCustomBackground(Color color) {
      this.customBackground = color;
      return (T)this;
   }

   public <T extends BoxWidget> T withCustomTheme(
      @Nullable Couple<Color> colorIdle, @Nullable Couple<Color> colorHover, @Nullable Couple<Color> colorClick, @Nullable Couple<Color> colorDisabled
   ) {
      if (colorIdle != null) {
         this.colorIdle = colorIdle;
      }

      if (colorHover != null) {
         this.colorHover = colorHover;
      }

      if (colorClick != null) {
         this.colorClick = colorClick;
      }

      if (colorDisabled != null) {
         this.colorDisabled = colorDisabled;
      }

      this.updateGradientFromState();
      return (T)this;
   }

   public <T extends BoxWidget> T animateColors(boolean b) {
      this.animateColors = b;
      return (T)this;
   }

   @Override
   public void tick() {
      super.tick();
      this.colorAnimation.tickChaser();
   }

   @Override
   public void onClick(double x, double y) {
      super.onClick(x, y);
      this.gradientColor = this.getColorClick();
      this.startGradientAnimation(this.getColorForState(), 0.15);
   }

   @Override
   protected void beforeRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.beforeRender(graphics, mouseX, mouseY, partialTicks);
      RenderSystem.enableDepthTest();
      if (this.isHovered != this.wasHovered) {
         this.animateGradientFromState();
      }

      if (this.colorAnimation.settled()) {
         this.gradientColor = this.gradientTarget;
      } else {
         float animationValue = 1.0F - Math.abs(this.colorAnimation.getValue(partialTicks));
         this.gradientColor = this.previousGradient.mapWithParams((prev, target) -> prev.mixWith(target, animationValue), this.gradientTarget);
      }
   }

   @Override
   public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      float fadeValue = this.fade.getValue(partialTicks);
      if (!(fadeValue < 0.1F)) {
         this.box.withAlpha(fadeValue);
         this.box
            .<BoxElement>withBackground(this.customBackground != null ? this.customBackground : BoxElement.COLOR_BACKGROUND_TRANSPARENT)
            .<BoxElement>gradientBorder(this.gradientColor)
            .<RenderElement>at((float)this.getX(), (float)this.getY(), this.z)
            .<RenderElement>withBounds(this.width, this.height)
            .render(graphics);
         super.doRender(graphics, mouseX, mouseY, partialTicks);
         this.wasHovered = this.isHovered;
      }
   }

   public boolean isMouseOver(double mX, double mY) {
      if (this.active && this.visible) {
         float padX = 2.0F + this.paddingX;
         float padY = 2.0F + this.paddingY;
         return (double)((float)this.getX() - padX) <= mX
            && (double)((float)this.getY() - padY) <= mY
            && mX < (double)((float)this.getX() + padX + (float)this.width)
            && mY < (double)((float)this.getY() + padY + (float)this.height);
      } else {
         return false;
      }
   }

   @Override
   protected boolean clicked(double pMouseX, double pMouseY) {
      return this.active && this.visible ? this.isMouseOver(pMouseX, pMouseY) : false;
   }

   public BoxElement getBox() {
      return this.box;
   }

   public void updateGradientFromState() {
      this.gradientTarget = this.getColorForState();
   }

   public void animateGradientFromState() {
      this.startGradientAnimation(this.getColorForState());
   }

   protected void startGradientAnimation(Couple<Color> target, double expSpeed) {
      if (this.animateColors) {
         this.colorAnimation.startWithValue(1.0);
         this.colorAnimation.chase(0.0, expSpeed, LerpedFloat.Chaser.EXP);
         this.colorAnimation.tickChaser();
         this.previousGradient = this.gradientColor;
         this.gradientTarget = target;
      }
   }

   protected void startGradientAnimation(Couple<Color> target) {
      this.startGradientAnimation(target, 0.6);
   }

   protected Couple<Color> getColorForState() {
      if (!this.active) {
         return this.getColorDisabled();
      } else if (this.customBorder != null) {
         return this.isHovered ? this.customBorder.map(Color::darker) : this.customBorder;
      } else {
         return this.isHovered ? this.getColorHover() : this.getColorIdle();
      }
   }

   public Couple<Color> getColorIdle() {
      return this.colorIdle;
   }

   public Couple<Color> getColorHover() {
      return this.colorHover;
   }

   public Couple<Color> getColorClick() {
      return this.colorClick;
   }

   public Couple<Color> getColorDisabled() {
      return this.colorDisabled;
   }
}
