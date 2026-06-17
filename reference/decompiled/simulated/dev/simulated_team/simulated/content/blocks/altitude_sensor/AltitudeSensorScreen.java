package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.network.packets.ConfigureAltitudeSensorPacket;
import dev.simulated_team.simulated.util.SimColors;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

public class AltitudeSensorScreen extends AbstractSimiScreen {
   private static final SimGUITextures BACKGROUND = SimGUITextures.ALTITUDE_SENSOR;
   private static final SimGUITextures BAR = SimGUITextures.ALTITUDE_SENSOR_BAR_LIT;
   private static final SimGUITextures GRABBY = SimGUITextures.ALTITUDE_SENSOR_GRABBY_THING;
   private final AltitudeSensorBlockEntity blockEntity;
   private final LerpedFloat visualHighSignal;
   private final LerpedFloat visualLowSignal;
   private final float lerpSpeed = 0.85F;
   private final int barCenterWidth = 8;
   private final int barWidth = 13;
   private final int barHeight = 200;
   private int barLeft = this.guiLeft + 3;
   private int barTop = this.guiTop + 3;
   private int rightBarLeft = this.guiLeft + 28;
   private int soundStep;
   private int ticksOpen;
   private float highSignal;
   private float lowSignal;
   boolean dragging;
   boolean draggingLeft;
   boolean draggingRight;

   public AltitudeSensorScreen(AltitudeSensorBlockEntity blockEntity) {
      super(SimLang.translate("gui.altitude_sensor.title").component());
      this.blockEntity = blockEntity;
      this.highSignal = blockEntity.highSignal;
      this.lowSignal = blockEntity.lowSignal;
      this.visualHighSignal = LerpedFloat.linear().startWithValue((double)this.highSignal);
      this.visualLowSignal = LerpedFloat.linear().startWithValue((double)this.lowSignal);
   }

   public static void open(AltitudeSensorBlockEntity blockEntity) {
      ScreenOpener.open(new AltitudeSensorScreen(blockEntity));
   }

   protected void init() {
      super.init();
      this.guiLeft = this.guiLeft - BACKGROUND.width / 2;
      this.guiTop = this.guiTop - BACKGROUND.height / 2;
      this.barLeft = this.guiLeft + 3;
      this.barTop = this.guiTop + 3;
      this.rightBarLeft = this.guiLeft + 28;
   }

   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      int a = (int)(80.0F * Math.min(1.0F, ((float)this.ticksOpen + AnimationTickHolder.getPartialTicks()) / 20.0F)) << 24;
      graphics.fillGradient(0, 0, this.width, this.height, 1052688 | a, 1052688 | a);
      BACKGROUND.render(graphics, this.guiLeft, this.guiTop);
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      float visualHighPT = this.visualHighSignal.getValue(partialTicks);
      float visualLowPT = this.visualLowSignal.getValue(partialTicks);
      float invHighSignal = 1.0F - visualHighPT;
      float invLowSignal = 1.0F - visualLowPT;
      int middleBarWidth = 10;
      int x = this.width / 2 - 5;
      int y = this.height / 2 - 200 / 2;
      int highMax = (int)(visualHighPT * 200.0F);
      int lowMax = (int)(visualLowPT * 200.0F);
      if (this.lowSignal > this.highSignal) {
         graphics.blit(BAR.location, x, y + BAR.height - highMax, BAR.startX, BAR.height - highMax - BAR.startY, BAR.width, BAR.height - (BAR.height - highMax));
      } else {
         graphics.blit(BAR.location, x, y, BAR.startX, BAR.startY, BAR.width, BAR.height - highMax);
      }

      PoseStack ps = graphics.pose();
      BAR.bind();
      RenderSystem.disableCull();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      float imageSize = 256.0F;
      float uvx1 = (float)BAR.startX / 256.0F;
      float uvx2 = (float)(BAR.startX + BAR.width) / 256.0F;
      float uvy1 = (float)(BAR.startY + highMax) / 256.0F;
      float uvy2 = (float)(BAR.startY + lowMax) / 256.0F;
      float px1 = (float)x;
      float px2 = (float)x + (float)BAR.width;
      float py1 = (float)(y - highMax + BAR.height);
      float py2 = (float)(y - lowMax + BAR.height);
      bufferbuilder.addVertex(ps.last().pose(), px2, py1, 0.0F).setUv(uvx2, uvy1).setColor(1.0F, 1.0F, 1.0F, 1.0F);
      bufferbuilder.addVertex(ps.last().pose(), px1, py1, 0.0F).setUv(uvx1, uvy1).setColor(1.0F, 1.0F, 1.0F, 1.0F);
      bufferbuilder.addVertex(ps.last().pose(), px1, py2, 0.0F).setUv(uvx1, uvy2).setColor(1.0F, 1.0F, 1.0F, 0.0F);
      bufferbuilder.addVertex(ps.last().pose(), px2, py2, 0.0F).setUv(uvx2, uvy2).setColor(1.0F, 1.0F, 1.0F, 0.0F);
      BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      int invHighMax = (int)(invHighSignal * 200.0F);
      int invLowMax = (int)(invLowSignal * 200.0F);
      GRABBY.render(graphics, this.guiLeft - 13 / 2, this.barTop - GRABBY.height / 2 + invLowMax);
      GRABBY.render(graphics, this.rightBarLeft - 13 / 2, this.barTop - GRABBY.height / 2 + invHighMax);
      int worldHigh = (int)this.blockEntity.toWorldHeight(this.highSignal);
      int worldLow = (int)this.blockEntity.toWorldHeight(this.lowSignal);
      String lowText = String.valueOf(worldLow);
      String highText = String.valueOf(worldHigh);
      graphics.drawCenteredString(
         this.font,
         lowText,
         this.barLeft + 8 / 2,
         this.barTop - 9 / 2 + invLowMax,
         !this.draggingLeft && !this.overGrabby((double)mouseX, (double)mouseY, true) ? SimColors.WOODEN_BROWN : SimColors.OFF_WHITE
      );
      graphics.drawCenteredString(
         this.font,
         highText,
         this.rightBarLeft + 13 / 2 + 1,
         this.barTop - 9 / 2 + invHighMax,
         !this.draggingRight && !this.overGrabby((double)mouseX, (double)mouseY, false) ? SimColors.WOODEN_BROWN : SimColors.OFF_WHITE
      );
      int textWidth = this.font.width(this.title);
      int textX = this.guiLeft - textWidth / 2 - 10;
      graphics.drawCenteredString(this.font, this.title, textX, this.height / 2 - 9, SimColors.OFF_WHITE);
   }

   private boolean overBar(double mouseX, double mouseY, boolean left) {
      int x = left ? this.guiLeft : this.rightBarLeft + 1;
      int y = this.barTop;
      return mouseX > (double)x && mouseX < (double)(x + 13) && mouseY > (double)y && mouseY < (double)(y + 200);
   }

   private boolean overGrabby(double mouseX, double mouseY, boolean left) {
      float visualHighPT = this.visualHighSignal.getValue(0.0F);
      float visualLowPT = this.visualLowSignal.getValue(0.0F);
      float invHighSignal = 1.0F - visualHighPT;
      float invLowSignal = 1.0F - visualLowPT;
      int invHighMax = (int)(invHighSignal * 200.0F);
      int invLowMax = (int)(invLowSignal * 200.0F);
      int x = (left ? this.barLeft : this.rightBarLeft) - 13 / 2;
      int y = this.barTop - GRABBY.height / 2 + (left ? invLowMax : invHighMax);
      return mouseX > (double)x && mouseX < (double)(x + GRABBY.width) && mouseY > (double)y && mouseY < (double)(y + GRABBY.height);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      this.draggingLeft = this.overGrabby(mouseX, mouseY, true);
      this.draggingRight = this.overGrabby(mouseX, mouseY, false);
      return super.mouseClicked(mouseX, mouseY, button);
   }

   public void mouseMoved(double mouseX, double mouseY) {
      if (this.draggingLeft || this.draggingRight) {
         this.updateValues(mouseX, mouseY);
      }

      super.mouseMoved(mouseX, mouseY);
   }

   private void updateValues(double mouseX, double mouseY) {
      int barTop = this.guiTop + 3;
      int barHeight = 200;
      float mouseProgress = (float)Mth.clamp(1.0 - (mouseY - (double)barTop) / 200.0, 0.0, 1.0);
      float change;
      if (hasControlDown()) {
         change = this.draggingLeft ? mouseProgress - this.lowSignal : mouseProgress - this.highSignal;
      } else {
         change = 0.0F;
      }

      if (!this.outOfBounds(this.lowSignal + change) && !this.outOfBounds(this.highSignal + change)) {
         if (this.draggingLeft) {
            this.lowSignal = mouseProgress;
            this.highSignal += change;
         } else if (this.draggingRight) {
            this.highSignal = mouseProgress;
            this.lowSignal += change;
         }

         this.visualHighSignal.chase((double)this.highSignal, 0.85F, Chaser.EXP);
         this.visualLowSignal.chase((double)this.lowSignal, 0.85F, Chaser.EXP);
         int soundSteps = 15;
         double newSoundStep = Math.floor((double)(mouseProgress * 15.0F));
         if (newSoundStep != (double)this.soundStep) {
            this.soundStep = (int)newSoundStep;
            Minecraft.getInstance().player.playSound(SoundEvents.LEVER_CLICK, 0.2F, 0.25F + mouseProgress * 0.5F);
         }
      }
   }

   public boolean outOfBounds(float value) {
      return value < 0.0F || value > 1.0F;
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.draggingLeft = false;
      this.draggingRight = false;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public void tick() {
      this.ticksOpen++;
      this.visualHighSignal.tickChaser();
      this.visualLowSignal.tickChaser();
   }

   public void onClose() {
      VeilPacketManager.server()
         .sendPacket(new CustomPacketPayload[]{new ConfigureAltitudeSensorPacket(this.blockEntity.getBlockPos(), this.highSignal, this.lowSignal)});
      super.onClose();
   }
}
