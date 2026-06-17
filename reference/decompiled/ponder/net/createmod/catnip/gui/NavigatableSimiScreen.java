package net.createmod.catnip.gui;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Matrix4f;

public abstract class NavigatableSimiScreen extends AbstractSimiScreen {
   public static final Couple<Color> COLOR_NAV_ARROW = Couple.create(new Color(-2136303207, true), new Color(816486809)).map(Color::setImmutable);
   protected static boolean currentlyRenderingPreviousScreen = false;
   protected int depthPointX;
   protected int depthPointY;
   public final LerpedFloat transition = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.1F, LerpedFloat.Chaser.LINEAR);
   protected final LerpedFloat arrowAnimation = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.075F, LerpedFloat.Chaser.LINEAR);
   @Nullable
   protected BoxWidget backTrack;

   public NavigatableSimiScreen() {
      Window window = Minecraft.getInstance().getWindow();
      this.depthPointX = window.getGuiScaledWidth() / 2;
      this.depthPointY = window.getGuiScaledHeight() / 2;
   }

   public void onClose() {
      ScreenOpener.clearStack();
      super.onClose();
   }

   @Override
   public void tick() {
      super.tick();
      this.transition.tickChaser();
      this.arrowAnimation.tickChaser();
   }

   @Override
   protected void init() {
      super.init();
      this.backTrack = null;
      List<Screen> screenHistory = ScreenOpener.getScreenHistory();
      if (!screenHistory.isEmpty()) {
         this.addRenderableWidget(
            this.backTrack = new BoxWidget(31, this.height - 31 - 20)
               .<BoxWidget>withBounds(20, 20)
               .<BoxWidget>withCustomBackground(BoxElement.COLOR_BACKGROUND_FLAT)
               .<ElementWidget>enableFade(0, 5)
               .<ElementWidget>withPadding(2.0F, 2.0F)
               .<ElementWidget>fade(1.0F)
               .withCallback(() -> ScreenOpener.openPreviousScreen(this, null))
         );
         Screen previousScreen = screenHistory.get(0);
         if (previousScreen instanceof NavigatableSimiScreen screen) {
            screen.initBackTrackIcon(this.backTrack);
         } else {
            this.backTrack.showing(PonderGuiTextures.ICON_DISABLE);
         }
      }
   }

   protected abstract void initBackTrackIcon(BoxWidget var1);

   protected Component backTrackingComponent() {
      return ScreenOpener.getBackStepScreen() instanceof NavigatableSimiScreen
         ? Lang.builder("catnip").translate("gui.step_back").component()
         : Lang.builder("catnip").translate("gui.exit").component();
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.backTrack != null) {
         PoseStack poseStack = graphics.pose();
         int x = (int)Mth.lerp(this.arrowAnimation.getValue(partialTicks), -9.0F, 21.0F);
         int maxX = this.backTrack.getX() + this.backTrack.getWidth();
         Couple<Color> colors = COLOR_NAV_ARROW;
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.0F, -300.0F);
         if (x + 30 < this.backTrack.getX()) {
            UIRenderHelper.breadcrumbArrow(graphics, x + 30, this.height - 51, 0, maxX - (x + 30), 20, 5, colors);
         }

         UIRenderHelper.breadcrumbArrow(graphics, x, this.height - 51, 0, 30, 20, 5, colors);
         UIRenderHelper.breadcrumbArrow(graphics, x - 30, this.height - 51, 0, 30, 20, 5, colors);
         poseStack.popPose();
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.0F, 500.0F);
         if (this.backTrack.isHoveredOrFocused()) {
            Component component = this.backTrackingComponent();
            graphics.drawString(
               this.font, component, 41 - this.font.width(component) / 2, this.height - 16, UIRenderHelper.COLOR_TEXT_DARKER.getFirst().getRGB(), false
            );
            if (Mth.equal(this.arrowAnimation.getValue(), this.arrowAnimation.getChaseTarget())) {
               this.arrowAnimation.setValue(1.0);
               this.arrowAnimation.setValue(1.0);
            }
         }

         poseStack.popPose();
      }
   }

   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      if (!isCurrentlyRenderingPreviousScreen()) {
         super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
      }
   }

   @Override
   protected void renderWindowBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.transition.getChaseTarget() != 0.0F && !this.transition.settled()) {
         this.renderBackground(graphics, mouseX, mouseY, partialTicks);
         PoseStack ms = graphics.pose();
         Window window = this.minecraft.getWindow();
         float guiScaledWidth = (float)window.getGuiScaledWidth();
         float guiScaledHeight = (float)window.getGuiScaledHeight();
         Screen lastScreen = ScreenOpener.getPreviouslyRenderedScreen();
         float tValue = this.transition.getValue(partialTicks);
         float tValueAbsolute = Math.abs(tValue);
         if (lastScreen != null && lastScreen != this && !this.transition.settled()) {
            currentlyRenderingPreviousScreen = true;
            ms.pushPose();
            UIRenderHelper.framebuffer.clear(Minecraft.ON_OSX);
            UIRenderHelper.framebuffer.bindWrite(true);
            lastScreen.render(graphics, 0, 0, partialTicks);
            ms.popPose();
            ms.pushPose();
            this.minecraft.getMainRenderTarget().bindWrite(true);
            int dpx = (int)(guiScaledWidth / 2.0F);
            int dpy = (int)(guiScaledHeight / 2.0F);
            if (lastScreen instanceof NavigatableSimiScreen navigableScreen && tValue > 0.0F) {
               dpx = navigableScreen.depthPointX;
               dpy = navigableScreen.depthPointY;
            }

            float scale = 1.0F + 0.2F * tValue;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Matrix4f matrix4f = new Matrix4f().setOrtho(0.0F, guiScaledWidth, guiScaledHeight, 0.0F, 1000.0F, 3000.0F);
            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().pose().set(matrix4f);
            poseStack2.translate((float)dpx, (float)dpy, 0.0F);
            poseStack2.scale(scale, scale, 1.0F);
            poseStack2.translate((float)(-dpx), (float)(-dpy), 0.0F);
            UIRenderHelper.drawFramebuffer(poseStack2, 1.0F - tValueAbsolute);
            RenderSystem.disableBlend();
            ms.popPose();
            currentlyRenderingPreviousScreen = false;
         }

         float scale = tValue > 0.0F ? 1.0F - 0.5F * (1.0F - tValueAbsolute) : 1.0F + 0.5F * (1.0F - tValueAbsolute);
         int dpx = (int)(guiScaledWidth / 2.0F);
         int dpy = (int)(guiScaledHeight / 2.0F);
         ms.translate((float)dpx, (float)dpy, 0.0F);
         ms.scale(scale, scale, 1.0F);
         ms.translate((float)(-dpx), (float)(-dpy), 0.0F);
      } else {
         this.renderBackground(graphics, mouseX, mouseY, partialTicks);
      }
   }

   @Override
   public boolean keyPressed(int code, int p_keyPressed_2_, int p_keyPressed_3_) {
      if (code == 259) {
         ScreenOpener.openPreviousScreen(this, null);
         return true;
      } else {
         return super.keyPressed(code, p_keyPressed_2_, p_keyPressed_3_);
      }
   }

   public void centerScalingOn(int x, int y) {
      this.depthPointX = x;
      this.depthPointY = y;
   }

   public void centerScalingOnMouse() {
      Window w = this.minecraft.getWindow();
      double mouseX = this.minecraft.mouseHandler.xpos() * (double)w.getGuiScaledWidth() / (double)w.getScreenWidth();
      double mouseY = this.minecraft.mouseHandler.ypos() * (double)w.getGuiScaledHeight() / (double)w.getScreenHeight();
      this.centerScalingOn((int)mouseX, (int)mouseY);
   }

   public boolean isEquivalentTo(NavigatableSimiScreen other) {
      return false;
   }

   public void shareContextWith(NavigatableSimiScreen other) {
   }

   protected void renderZeloBreadcrumbs(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      List<Screen> history = ScreenOpener.getScreenHistory();
      if (!history.isEmpty()) {
         history.add(0, this.minecraft.screen);
         int spacing = 20;
         List<String> names = new ArrayList<>();

         for (Screen screen : history) {
            names.add(screenTitle(screen));
         }

         int bWidth = 0;

         for (String name : names) {
            bWidth += this.font.width(name) + spacing;
         }

         MutableInt x = new MutableInt(this.width - bWidth);
         MutableInt y = new MutableInt(this.height - 18);
         MutableBoolean first = new MutableBoolean(true);
         if (x.getValue() < 25) {
            x.setValue(25);
         }

         PoseStack poseStack = graphics.pose();
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.0F, 600.0F);
         names.forEach(
            s -> {
               int sWidth = this.font.width(s);
               UIRenderHelper.breadcrumbArrow(
                  graphics, x.getValue(), y.getValue(), 0, sWidth + spacing, 14, spacing / 2, new Color(-586149872), new Color(1141903376)
               );
               graphics.drawString(this.font, s, x.getValue() + 5, y.getValue() + 3, first.getValue() ? -1114130 : -2232577);
               first.setFalse();
               x.add(sWidth + spacing);
            }
         );
         poseStack.popPose();
      }
   }

   public static boolean isCurrentlyRenderingPreviousScreen() {
      return currentlyRenderingPreviousScreen;
   }

   private static String screenTitle(Screen screen) {
      return screen instanceof NavigatableSimiScreen ? ((NavigatableSimiScreen)screen).getBreadcrumbTitle() : "<";
   }

   protected String getBreadcrumbTitle() {
      return this.getClass().getSimpleName();
   }
}
