package dev.simulated_team.simulated.ponder.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.gui.element.GuiGameElement.GuiRenderBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.InputWindowElement;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class KeybindWindowElement extends InputWindowElement {
   private final Vec3 sceneSpace;
   private final Pointing direction;
   @Nullable
   Component keybind;
   @Nullable
   ScreenElement icon;
   ItemStack item = ItemStack.EMPTY;

   public KeybindWindowElement(Vec3 sceneSpace, Pointing direction) {
      super(sceneSpace, direction);
      this.sceneSpace = sceneSpace;
      this.direction = direction;
   }

   @NotNull
   public KeybindWindowElement.Builder builder() {
      return new KeybindWindowElement.Builder();
   }

   public void render(@NotNull PonderScene scene, PonderUI screen, @NotNull GuiGraphics graphics, float partialTicks, float fade) {
      Font font = screen.getFontRenderer();
      int width = 0;
      int height = 0;
      float xFade = this.direction == Pointing.RIGHT ? -1.0F : (this.direction == Pointing.LEFT ? 1.0F : 0.0F);
      float yFade = this.direction == Pointing.DOWN ? -1.0F : (this.direction == Pointing.UP ? 1.0F : 0.0F);
      xFade *= 10.0F * (1.0F - fade);
      yFade *= 10.0F * (1.0F - fade);
      boolean hasItem = !this.item.isEmpty();
      boolean hasText = this.keybind != null;
      boolean hasIcon = this.icon != null;
      int keyWidth = 0;
      Component text = (Component)(hasText ? this.keybind : Component.empty());
      if (!(fade < 0.0625F)) {
         Vec2 sceneToScreen = scene.getTransform().sceneToScreen(this.sceneSpace, partialTicks);
         if (hasIcon) {
            width += 24;
            height = 24;
         }

         if (hasText) {
            keyWidth = font.width(text);
            width += keyWidth;
         }

         if (hasItem) {
            width += 24;
            height = 24;
         }

         PoseStack poseStack = graphics.pose();
         poseStack.pushPose();
         poseStack.translate(sceneToScreen.x + xFade, sceneToScreen.y + yFade, 400.0F);
         PonderUI.renderSpeechBox(graphics, 0, 0, width, height, false, this.direction, true);
         poseStack.translate(0.0F, 0.0F, 100.0F);
         if (hasText) {
            graphics.drawString(font, text, 2, (int)((float)(height - 9) / 2.0F + 2.0F), PonderPalette.WHITE.getColorObject().scaleAlpha(fade).getRGB(), false);
         }

         if (hasIcon) {
            poseStack.pushPose();
            poseStack.translate((float)keyWidth, 0.0F, 0.0F);
            poseStack.scale(1.5F, 1.5F, 1.5F);
            this.icon.render(graphics, 0, 0);
            poseStack.popPose();
         }

         if (hasItem) {
            ((GuiRenderBuilder)GuiGameElement.of(this.item).at((float)(keyWidth + (hasIcon ? 24 : 0)), 0.0F)).scale(1.5).render(graphics);
            RenderSystem.disableDepthTest();
         }

         poseStack.popPose();
      }
   }

   public class Builder implements InputElementBuilder {
      public KeybindWindowElement.Builder withItem(ItemStack stack) {
         KeybindWindowElement.this.item = stack;
         return this;
      }

      public KeybindWindowElement.Builder leftClick() {
         KeybindWindowElement.this.icon = PonderGuiTextures.ICON_LMB;
         return this;
      }

      public KeybindWindowElement.Builder scroll() {
         KeybindWindowElement.this.icon = PonderGuiTextures.ICON_SCROLL;
         return this;
      }

      public KeybindWindowElement.Builder rightClick() {
         KeybindWindowElement.this.icon = PonderGuiTextures.ICON_RMB;
         return this;
      }

      public KeybindWindowElement.Builder showing(ScreenElement icon) {
         KeybindWindowElement.this.icon = icon;
         return this;
      }

      public KeybindWindowElement.Builder whileSneaking() {
         throw new UnsupportedOperationException();
      }

      public KeybindWindowElement.Builder whileCTRL() {
         throw new UnsupportedOperationException();
      }

      public KeybindWindowElement.Builder keybind(String keybind) {
         KeybindWindowElement.this.keybind = Component.keybind(keybind).append(" +");
         return this;
      }
   }
}
