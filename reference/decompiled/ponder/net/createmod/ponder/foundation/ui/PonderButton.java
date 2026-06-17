package net.createmod.ponder.foundation.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Locale;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.foundation.PonderTag;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class PonderButton extends BoxWidget {
   public static final Couple<Color> COLOR_IDLE = Couple.create(new Color(1623245055, true), new Color(817938687, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_HOVER = Couple.create(new Color(-255803137, true), new Color(-1597980417, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_CLICK = Couple.create(new Color(-1, true), new Color(-570425345, true)).map(Color::setImmutable);
   public static final Couple<Color> COLOR_DISABLED = Couple.create(new Color(-2138009456, true), new Color(546345104, true)).map(Color::setImmutable);
   @Nullable
   protected ItemStack item;
   @Nullable
   protected PonderTag tag;
   @Nullable
   protected KeyMapping shortcut;
   protected LerpedFloat flash = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.1F, LerpedFloat.Chaser.EXP);

   public PonderButton(int x, int y) {
      this(x, y, 20, 20);
   }

   public PonderButton(int x, int y, int width, int height) {
      super(x, y, width, height);
      this.z = 420.0F;
      this.paddingX = 2.0F;
      this.paddingY = 2.0F;
      this.colorIdle = COLOR_IDLE;
      this.colorHover = COLOR_HOVER;
      this.colorClick = COLOR_CLICK;
      this.colorDisabled = COLOR_DISABLED;
      this.updateGradientFromState();
   }

   public <T extends PonderButton> T withShortcut(KeyMapping key) {
      this.shortcut = key;
      return (T)this;
   }

   public <T extends PonderButton> T showingTag(PonderTag tag) {
      return this.showing(this.tag = tag);
   }

   public <T extends PonderButton> T showing(ItemStack item) {
      this.item = item;
      return super.showingElement(GuiGameElement.of(item).scale(1.5).at(-4.0F, -4.0F));
   }

   public void flash() {
      this.flash.updateChaseTarget(1.0F);
   }

   public void dim() {
      this.flash.updateChaseTarget(0.0F);
   }

   @Override
   public void tick() {
      super.tick();
      this.flash.tickChaser();
   }

   @Override
   protected void beforeRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.beforeRender(graphics, mouseX, mouseY, partialTicks);
      float flashValue = this.flash.getValue(partialTicks);
      if (flashValue > 0.1F) {
         float sin = 0.5F + 0.5F * Mth.sin(((float)AnimationTickHolder.getTicks(true) + partialTicks) / 10.0F);
         sin *= flashValue;
         Color nc1 = new Color(255, 255, 255, Mth.clamp(this.gradientColor.getFirst().getAlpha() + 150, 0, 255));
         Color nc2 = new Color(155, 155, 155, Mth.clamp(this.gradientColor.getSecond().getAlpha() + 150, 0, 255));
         Couple<Color> newColors = Couple.create(nc1, nc2);
         this.gradientColor = this.gradientColor.mapWithParams((color, other) -> color.mixWith(other, sin), newColors);
      }
   }

   @Override
   public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.doRender(graphics, mouseX, mouseY, partialTicks);
      if (this.isVisible()) {
         if (this.shortcut != null) {
            PoseStack poseStack = graphics.pose();
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, this.z + 10.0F);
            graphics.drawCenteredString(
               Minecraft.getInstance().font,
               this.shortcut.getTranslatedKeyMessage().getString().toLowerCase(Locale.ROOT),
               this.getX() + this.width / 2 + 8,
               this.getY() + this.height - 6,
               UIRenderHelper.COLOR_TEXT_DARKER.getFirst().scaleAlpha(this.fade.getValue()).getRGB()
            );
            poseStack.popPose();
         }
      }
   }

   public boolean isFocused() {
      return false;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.shortcut != null && this.shortcut.matches(keyCode, scanCode)) {
         this.gradientColor = this.getColorClick();
         this.startGradientAnimation(this.getColorForState(), 0.15);
         this.runCallback((double)((float)this.width / 2.0F), (double)((float)this.height / 2.0F));
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   protected boolean isValidClickButton(int i) {
      return this.isVisible();
   }

   @Nullable
   public ItemStack getItem() {
      return this.item;
   }

   @Nullable
   public PonderTag getTag() {
      return this.tag;
   }

   public boolean isVisible() {
      return !(this.fade.getValue() < 0.1F);
   }
}
