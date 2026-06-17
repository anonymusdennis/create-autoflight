package net.createmod.catnip.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.gui.element.TextStencilElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.platform.CatnipClientServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class ConfirmationScreen extends AbstractSimiScreen {
   private Screen source;
   private Consumer<ConfirmationScreen.Response> action = _success -> {
   };
   private final List<FormattedText> text = new ArrayList<>();
   private boolean centered = false;
   private int x;
   private int y;
   private int textWidth;
   private int textHeight;
   private boolean tristate;
   private BoxWidget confirm;
   private BoxWidget confirmDontSave;
   private BoxWidget cancel;
   private BoxElement textBackground;

   public ConfirmationScreen removeTextLines(int amount) {
      if (amount > this.text.size()) {
         return this.clearText();
      } else {
         this.text.subList(this.text.size() - amount, this.text.size()).clear();
         return this;
      }
   }

   public ConfirmationScreen clearText() {
      this.text.clear();
      return this;
   }

   public ConfirmationScreen addText(FormattedText text) {
      this.text.add(text);
      return this;
   }

   public ConfirmationScreen withText(FormattedText text) {
      return this.clearText().addText(text);
   }

   public ConfirmationScreen at(int x, int y) {
      this.x = Math.max(x, 0);
      this.y = Math.max(y, 0);
      this.centered = false;
      return this;
   }

   public ConfirmationScreen centered() {
      this.centered = true;
      return this;
   }

   public ConfirmationScreen withAction(Consumer<Boolean> action) {
      this.action = r -> action.accept(r == ConfirmationScreen.Response.Confirm);
      return this;
   }

   public ConfirmationScreen withThreeActions(Consumer<ConfirmationScreen.Response> action) {
      this.action = action;
      this.tristate = true;
      return this;
   }

   public void open(@Nonnull Screen source) {
      this.source = source;
      Minecraft client = CatnipClientServices.CLIENT_HOOKS.getMinecraftFromScreen(source);
      this.init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
      this.minecraft.screen = this;
   }

   @Override
   public void tick() {
      super.tick();
      this.source.tick();
   }

   @Override
   protected void init() {
      super.init();
      ArrayList<FormattedText> copy = new ArrayList<>(this.text);
      this.text.clear();
      copy.forEach(t -> this.text.addAll(this.font.getSplitter().splitLines(t, 300, Style.EMPTY)));
      this.textHeight = this.text.size() * (9 + 1) + 4;
      this.textWidth = 300;
      if (this.centered) {
         this.x = this.width / 2 - this.textWidth / 2 - 2;
         this.y = this.height / 2 - this.textHeight / 2 - 16;
      } else {
         this.x = Math.max(0, this.x - this.textWidth / 2);
         this.y = Math.max(0, this.y = this.y - this.textHeight);
      }

      if (this.x + this.textWidth > this.width) {
         this.x = this.width - this.textWidth;
      }

      if (this.y + this.textHeight + 30 > this.height) {
         this.y = this.height - this.textHeight - 30;
      }

      int buttonX = this.x + this.textWidth / 2 - 6 - (int)(70.0F * (this.tristate ? 1.5F : 1.0F));
      TextStencilElement confirmText = new TextStencilElement(
            this.font, Component.translatable(this.tristate ? "catnip.ui.save_label" : "catnip.ui.confirm_label")
         )
         .centered(true, true);
      this.confirm = new BoxWidget(buttonX, this.y + this.textHeight + 6, 70, 16).withCallback(() -> this.accept(ConfirmationScreen.Response.Confirm));
      this.confirm.showingElement(confirmText.withElementRenderer(BoxWidget.gradientFactory.apply(this.confirm)));
      this.addRenderableWidget(this.confirm);
      buttonX += 82;
      if (this.tristate) {
         TextStencilElement confirmDontSaveText = new TextStencilElement(this.font, Component.translatable("catnip.ui.dont_save_label")).centered(true, true);
         this.confirmDontSave = new BoxWidget(buttonX, this.y + this.textHeight + 6, 70, 16)
            .withCallback(() -> this.accept(ConfirmationScreen.Response.ConfirmDontSave));
         this.confirmDontSave.showingElement(confirmDontSaveText.withElementRenderer(BoxWidget.gradientFactory.apply(this.confirmDontSave)));
         this.addRenderableWidget(this.confirmDontSave);
         buttonX += 82;
      }

      TextStencilElement cancelText = new TextStencilElement(this.font, Component.translatable("catnip.ui.cancel_label")).centered(true, true);
      this.cancel = new BoxWidget(buttonX, this.y + this.textHeight + 6, 70, 16).withCallback(() -> this.accept(ConfirmationScreen.Response.Cancel));
      this.cancel.showingElement(cancelText.withElementRenderer(BoxWidget.gradientFactory.apply(this.cancel)));
      this.addRenderableWidget(this.cancel);
      this.textBackground = new BoxElement()
         .<BoxElement>withBackground(BoxElement.COLOR_BACKGROUND_FLAT)
         .<BoxElement>gradientBorder(AbstractSimiWidget.COLOR_DISABLED)
         .<RenderElement>withBounds(this.width + 10, this.textHeight + 35)
         .at(-5.0F, (float)(this.y - 5));
      if (this.text.size() == 1) {
         this.x = (this.width - this.font.width(this.text.get(0))) / 2;
      }
   }

   public void onClose() {
      this.accept(ConfirmationScreen.Response.Cancel);
   }

   private void accept(ConfirmationScreen.Response success) {
      this.minecraft.screen = this.source;
      this.action.accept(success);
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.textBackground.render(graphics);
      int offset = 9 + 1;
      int lineY = this.y - offset;
      PoseStack poseStack = graphics.pose();
      poseStack.pushPose();
      poseStack.translate(0.0F, 0.0F, 200.0F);

      for (FormattedText line : this.text) {
         lineY += offset;
         if (line != null) {
            graphics.drawString(this.font, line.getString(), this.x, lineY, 15395562, false);
         }
      }

      poseStack.popPose();
   }

   @Override
   protected void renderWindowBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      this.endFrame();
      this.source.render(graphics, 0, 0, 10.0F);
      this.prepareFrame();
      graphics.fillGradient(0, 0, this.width, this.height, 1880100880, -2146430960);
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

   public void resize(@Nonnull Minecraft client, int width, int height) {
      super.resize(client, width, height);
      this.source.resize(client, width, height);
   }

   @Override
   public boolean isPauseScreen() {
      return true;
   }

   public static enum Response {
      Confirm,
      ConfirmDontSave,
      Cancel;
   }
}
