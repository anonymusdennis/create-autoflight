package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import dev.simulated_team.simulated.network.packets.ThrottleLeverSignalPacket;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.UIRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ThrottleLeverHandler extends BlockHoldInteraction {
   protected boolean inverted = false;
   protected int lastSignal = 0;
   protected int signal = 0;
   protected float value = 0.0F;
   protected float animatedValue;
   protected float lastAnimatedValue;

   @Override
   public void startHold(Level level, Player player, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof ThrottleLeverBlockEntity be) {
         this.lastSignal = be.state;
         this.inverted = (Boolean)be.getBlockState().getValue(ThrottleLeverBlock.INVERTED);
         this.signal = this.inverted ? 15 - be.state : be.state;
         this.value = (float)this.signal / 15.0F;
         this.animatedValue = this.lastAnimatedValue = this.value;
      }

      super.startHold(level, player, pos);
   }

   @Override
   public boolean activeTick(Level level, LocalPlayer player) {
      if (level.getBlockEntity(this.getInteractionPos()) instanceof ThrottleLeverBlockEntity
         && BlockHoldInteraction.inInteractionRange(player, this.getInteractionPos().getCenter(), 0.0)) {
         float speed = 0.85F;
         this.lastAnimatedValue = this.animatedValue;
         this.animatedValue = this.animatedValue * 0.14999998F + (float)this.signal / 15.0F * 0.85F;
         return false;
      } else {
         return true;
      }
   }

   @Override
   public void renderOverlay(GuiGraphics graphics, int width, int height, boolean hideGui) {
      if (!hideGui) {
         int h = 14;
         int w = 100;
         int x = width / 2 - 50 + 16;
         int y = height / 2 - 7;
         PoseStack ps = graphics.pose();
         ps.pushPose();
         ps.translate((float)(x + 50), (float)(y + 7), 0.0F);
         ps.mulPose(Axis.ZP.rotationDegrees(90.0F));
         ps.translate((float)(-x - 50), (float)(-y - 7), 0.0F);
         AllGuiTextures.BRASS_FRAME_TL.render(graphics, x, y);
         AllGuiTextures.BRASS_FRAME_TR.render(graphics, x + 100 - 4, y);
         AllGuiTextures.BRASS_FRAME_BL.render(graphics, x, y + 14 - 4);
         AllGuiTextures.BRASS_FRAME_BR.render(graphics, x + 100 - 4, y + 14 - 4);
         int zLevel = 2;
         UIRenderHelper.drawStretched(graphics, x, y + 4, 3, 6, 2, AllGuiTextures.BRASS_FRAME_LEFT);
         UIRenderHelper.drawStretched(graphics, x + 100 - 3, y + 4, 3, 6, 2, AllGuiTextures.BRASS_FRAME_RIGHT);
         UIRenderHelper.drawCropped(graphics, x + 4, y, 92, 3, 2, AllGuiTextures.BRASS_FRAME_TOP);
         UIRenderHelper.drawCropped(graphics, x + 4, y + 14 - 3, 92, 3, 2, AllGuiTextures.BRASS_FRAME_BOTTOM);
         int valueBarX = x + 3;
         int valueBarWidth = 94;

         for (int w1 = 0; w1 < 94; w1 += AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1) {
            UIRenderHelper.drawCropped(
               graphics, valueBarX + w1, y + 3, Math.min(AllGuiTextures.VALUE_SETTINGS_BAR.getWidth() - 1, 94 - w1), 8, 2, AllGuiTextures.VALUE_SETTINGS_BAR
            );
         }

         ps.popPose();
         ps.pushPose();
         ps.translate(0.0, 0.0, 4.0);
         float partialTick = AnimationTickHolder.getPartialTicks();
         float currentValue = this.lastAnimatedValue * (1.0F - partialTick) + this.animatedValue * partialTick;
         float cursorY = (1.0F - 2.0F * currentValue) * 3.0F * 14.0F + 2.0F;
         int cx = x + 50 - 7;
         float cy = (float)(y + 7 - 9) + cursorY;
         int cursorWidth = 14;
         ps.pushPose();
         ps.translate(0.0F, cy, 0.0F);
         AllGuiTextures.VALUE_SETTINGS_CURSOR_LEFT.render(graphics, cx - 3, 0);
         UIRenderHelper.drawCropped(graphics, cx, 0, 14, 14, 2, AllGuiTextures.VALUE_SETTINGS_CURSOR);
         AllGuiTextures.VALUE_SETTINGS_CURSOR_RIGHT.render(graphics, cx + 14, 0);
         ps.translate(0.0, 0.0, 4.0);
         graphics.drawString(
            Minecraft.getInstance().font, String.valueOf(this.inverted ? 15 - this.signal : this.signal), cx + 1, 3, SimColors.THROTTLE_VALUE_BROWN, false
         );
         ps.popPose();
         ps.popPose();
      }
   }

   @Override
   public boolean activeOnMouseMove(double yaw, double pitch) {
      this.value -= (float)(pitch / 180.0);
      this.value = Math.min(1.0F, Math.max(0.0F, this.value));
      int newSignal = Math.round(this.value * 15.0F);
      this.signal = Math.min(15, Math.max(0, newSignal));
      if (this.signal != this.lastSignal) {
         this.lastSignal = this.signal;
         this.changed();
      }

      return true;
   }

   private void changed() {
      VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new ThrottleLeverSignalPacket(this.getInteractionPos(), this.signal)});
   }
}
