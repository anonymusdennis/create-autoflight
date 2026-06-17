package net.createmod.catnip.config.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.Force;
import net.createmod.catnip.animation.PhysicalFloat;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.TriConsumer;

public abstract class ConfigScreen extends AbstractSimiScreen {
   public static final Map<String, TriConsumer<Screen, GuiGraphics, Float>> backgrounds = new HashMap<>();
   public static final PhysicalFloat cogSpin = PhysicalFloat.create().withLimit(10.0F).withDrag(0.3).addForce(new Force.Static(0.2F));
   @Nullable
   public static String modID = null;
   @Nullable
   protected final Screen parent;
   public static BlockState shadowState = Blocks.POTTED_CRIMSON_ROOTS.defaultBlockState();
   public static DelegatedStencilElement shadowElement = new DelegatedStencilElement(
      (graphics, x, y, alpha) -> renderCog(graphics), (graphics, x, y, alpha) -> graphics.fill(-200, -200, 200, 200, 1610612736)
   );
   private static final PanoramaRenderer vanillaPanorama = new PanoramaRenderer(TitleScreen.CUBE_MAP);

   public ConfigScreen(@Nullable Screen parent) {
      this.parent = parent;
   }

   @Override
   public void tick() {
      super.tick();
      cogSpin.tick();
   }

   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
   }

   @Override
   protected void renderWindowBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.minecraft != null && this.minecraft.level != null) {
         graphics.fill(0, 0, this.width, this.height, -1339544524);
      } else {
         this.renderMenuBackground(graphics, partialTicks);
      }

      shadowElement.<RenderElement>at((float)this.width * 0.5F, (float)this.height * 0.5F, 0.0F).render(graphics);
      super.renderWindowBackground(graphics, mouseX, mouseY, partialTicks);
   }

   @Override
   protected void prepareFrame() {
      UIRenderHelper.swapAndBlitColor(this.minecraft.getMainRenderTarget(), UIRenderHelper.framebuffer);
      RenderSystem.clear(1280, Minecraft.ON_OSX);
   }

   @Override
   protected void endFrame() {
      UIRenderHelper.swapAndBlitColor(UIRenderHelper.framebuffer, this.minecraft.getMainRenderTarget());
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      cogSpin.bump(3, -scrollY * 5.0);
      return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   public static String toHumanReadable(String key) {
      String s = key.replaceAll("_", " ");
      s = Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(s)).<CharSequence>map(StringUtils::capitalize).collect(Collectors.joining(" "));
      return StringUtils.normalizeSpace(s);
   }

   protected void renderMenuBackground(GuiGraphics graphics, float partialTicks) {
      TriConsumer<Screen, GuiGraphics, Float> customBackground = backgrounds.get(modID);
      if (customBackground != null) {
         customBackground.accept(this, graphics, partialTicks);
      } else {
         vanillaPanorama.render(graphics, this.width, this.height, 1.0F, partialTicks);
         graphics.fill(0, 0, this.width, this.height, -1876415436);
      }
   }

   protected static void renderCog(GuiGraphics graphics) {
      float partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate(-100.0F, 100.0F, -100.0F);
      poseStack.scale(200.0F, 200.0F, 1.0F);
      GuiGameElement.of(shadowState).rotateBlock(22.5, (double)cogSpin.getValue(partialTicks), 22.5).render(graphics);
      poseStack.popPose();
   }
}
