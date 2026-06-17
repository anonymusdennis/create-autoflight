package dev.simulated_team.simulated.content.blocks.nameplate;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.network.packets.name_plate.NameplateChangeNamePacket;
import foundry.veil.api.network.VeilPacketManager;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.block.state.BlockState;

public class NameplateScreen extends Screen {
   public static final int MAX_WIDTH = 122;
   private final NameplateBlockEntity be;
   private String message;
   private int frame;
   @Nullable
   private TextFieldHelper nameField;
   private Button button;

   public NameplateScreen(NameplateBlockEntity pSign) {
      this(pSign, SimLang.translate("nameplate.edit").component());
   }

   public NameplateScreen(NameplateBlockEntity pSign, Component pTitle) {
      super(pTitle);
      this.be = pSign;
      this.message = pSign.getName();
   }

   protected void init() {
      this.button = (Button)this.addWidget(
         Button.builder(CommonComponents.GUI_DONE, button -> this.onDone()).bounds(this.width / 2 - 100, this.height / 4 + 144, 200, 20).build()
      );
      this.nameField = new TextFieldHelper(
         () -> this.message,
         this::setMessage,
         TextFieldHelper.createClipboardGetter(this.minecraft),
         TextFieldHelper.createClipboardSetter(this.minecraft),
         string -> this.minecraft.font.width(string) <= 122
      );
   }

   private void setMessage(String s) {
      this.message = s;
   }

   public void tick() {
      this.frame++;
      if (!this.isValid()) {
         this.onDone();
      }
   }

   private boolean isValid() {
      return this.minecraft != null
         && this.minecraft.player != null
         && !this.be.isRemoved()
         && NameplateBlockEntity.canPlayerReach(this.be, this.minecraft.player);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      return pKeyCode != 264 && pKeyCode != 257 && pKeyCode != 335
         ? this.nameField.keyPressed(pKeyCode) || super.keyPressed(pKeyCode, pScanCode, pModifiers)
         : false;
   }

   public boolean charTyped(char pCodePoint, int pModifiers) {
      this.nameField.charTyped(pCodePoint);
      return true;
   }

   public void render(GuiGraphics gui, int pMouseX, int pMouseY, float pPartialTick) {
      Lighting.setupForFlatItems();
      this.renderBackground(gui, pMouseX, pMouseY, pPartialTick);
      gui.drawCenteredString(this.font, this.title, this.width / 2, 40, 16777215);
      this.renderSign(gui);
      Lighting.setupFor3DItems();
      this.button.render(gui, pMouseX, pMouseY, pPartialTick);
   }

   public void onClose() {
      this.onDone();
   }

   public void removed() {
      VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new NameplateChangeNamePacket(this.be.findController().getBlockPos(), this.message)});
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected void renderSignBackground(GuiGraphics gui, BlockState pState) {
      String color = ((NameplateBlock)pState.getBlock()).getColor().getSerializedName();
      PoseStack ps = gui.pose();
      ps.pushPose();
      float sy = 1.25F;
      ps.scale(1.25F, 1.25F, 1.0F);
      ps.translate(-56.0, 5.7, 0.0);
      gui.blit(Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 0.0F, 12.0F, 16, 10, 32, 32);

      for (int i = 0; i < 6; i++) {
         ps.translate(16.0, 0.0, 0.0);
         gui.blit(Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 8.0F, 12.0F, 16, 10, 32, 32);
      }

      ps.translate(16.0, 0.0, 0.0);
      gui.blit(Simulated.path("textures/block/nameplate/" + color + "_nameplate.png"), -8, -8, 16.0F, 12.0F, 16, 10, 32, 32);
      ps.popPose();
   }

   protected void offsetSign(GuiGraphics pGuiGraphics, BlockState pState) {
      pGuiGraphics.pose().translate((float)this.width / 2.0F, (float)this.height / 2.0F - 26.0F, 50.0F);
   }

   private void renderSign(GuiGraphics pGuiGraphics) {
      PoseStack ps = pGuiGraphics.pose();
      ps.pushPose();
      BlockState blockstate = this.be.getBlockState();
      pGuiGraphics.pose().pushPose();
      this.offsetSign(pGuiGraphics, blockstate);
      float scale = 2.0F;
      ps.scale(2.0F, 2.0F, 2.0F);
      pGuiGraphics.pose().pushPose();
      this.renderSignBackground(pGuiGraphics, blockstate);
      pGuiGraphics.pose().popPose();
      this.renderSignText(pGuiGraphics);
      pGuiGraphics.pose().popPose();
      ps.popPose();
   }

   private void renderSignText(GuiGraphics pGuiGraphics) {
      int lineHeight = 8;
      pGuiGraphics.pose().translate(0.0F, 0.0F, 4.0F);
      int color = this.be.getDarkColor(this.be.getTextColor());
      boolean cursorFlash = this.frame / 6 % 2 == 0;
      int cursorPos = this.nameField.getCursorPos();
      int selectionPos = this.nameField.getSelectionPos();
      if (this.message != null) {
         if (this.font.isBidirectional()) {
            this.message = this.font.bidirectionalShaping(this.message);
         }

         int w = -this.font.width(this.message) / 2;
         pGuiGraphics.drawString(this.font, this.message, w, 0, color, false);
         if (cursorPos >= 0 && cursorFlash) {
            int l1 = this.font.width(this.message.substring(0, Math.max(Math.min(cursorPos, this.message.length()), 0)));
            int i2 = l1 - this.font.width(this.message) / 2;
            if (cursorPos >= this.message.length()) {
               pGuiGraphics.drawString(this.font, "_", i2, 0, color, false);
            }
         }
      }

      if (this.message != null && cursorPos >= 0) {
         int width = this.font.width(this.message.substring(0, Math.max(Math.min(cursorPos, this.message.length()), 0)));
         int cen = width - this.font.width(this.message) / 2;
         if (cursorFlash && cursorPos < this.message.length()) {
            pGuiGraphics.fill(cen, -1, cen + 1, 8, 0xFF000000 | color);
         }

         if (selectionPos != cursorPos) {
            int min = Math.min(cursorPos, selectionPos);
            int max = Math.max(cursorPos, selectionPos);
            int minWidth = this.font.width(this.message.substring(0, min)) - this.font.width(this.message) / 2;
            int maxWith = this.font.width(this.message.substring(0, max)) - this.font.width(this.message) / 2;
            int selMin = Math.min(minWidth, maxWith);
            int selMax = Math.max(minWidth, maxWith);
            pGuiGraphics.fill(RenderType.guiTextHighlight(), selMin, -1, selMax, 8, -16776961);
         }
      }
   }

   private void onDone() {
      this.minecraft.setScreen(null);
   }

   public static void setScreen(NameplateBlockEntity be) {
      if (be != null && NameplateBlockEntity.canPlayerReach(be, Minecraft.getInstance().player)) {
         NameplateScreen screen = new NameplateScreen(be.findController());
         Minecraft.getInstance().setScreen(screen);
      }
   }
}
