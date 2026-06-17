package dev.simulated_team.simulated.content.blocks.physics_assembler;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.util.SableDistUtil;
import dev.simulated_team.simulated.index.SimGUITextures;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.network.packets.AssemblePacket;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PhysicsAssemblerGUIHandler extends BlockHoldInteraction {
   private static final double PULLED_THRESHOLD = 0.015;
   public static int lastSignal = 0;
   public static float animatedVelocity;
   public static float animatedValue;
   public static float lastAnimatedValue;

   @Override
   public void startHold(Level level, Player player, BlockPos blockPos) {
      super.startHold(level, player, blockPos);
      if (level.getBlockEntity(blockPos) instanceof PhysicsAssemblerBlockEntity assembler) {
         animatedValue = 0.0F;
         if (Sable.HELPER.getContaining(assembler) != null) {
            animatedValue = 1.0F;
         }

         animatedVelocity = 0.0F;
      }
   }

   @Override
   public void release() {
      if (SableDistUtil.getClientLevel().getBlockEntity(this.getInteractionPos()) instanceof PhysicsAssemblerBlockEntity be) {
         if (be.holdingLever) {
            return;
         }

         boolean inPlot = this.getSubLevelHolding() != null;
         if (inPlot && (double)animatedValue < 0.015 && (double)lastAnimatedValue < 0.015
            || !inPlot && (double)lastAnimatedValue > 0.985 && (double)animatedValue > 0.985) {
            VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new AssemblePacket(this.getInteractionPos())});
            inPlot = !inPlot;
            be.setClientHoldLeverInPlace(true);
         }

         be.visualAngle.setValue((double)animatedValue * 45.0);
         be.clientFlickLeverTo(inPlot);
         be.stopControllingPlayer();
      }
   }

   @Override
   public boolean activeTick(Level level, LocalPlayer player) {
      if (level == null) {
         return true;
      } else if (level.getBlockEntity(this.getInteractionPos()) instanceof PhysicsAssemblerBlockEntity be) {
         if (BlockHoldInteraction.inInteractionRange(player, this.getInteractionPos().getCenter(), 2.0)) {
            lastAnimatedValue = animatedValue;
            animatedValue = animatedValue + animatedVelocity;
            animatedVelocity *= 0.8F;
            be.updateControlledByPlayer(animatedValue * 45.0F);
            return false;
         } else {
            boolean inPlot = this.getSubLevelHolding() != null;
            be.visualAngle.setValue((double)animatedValue * 45.0);
            be.clientFlickLeverTo(inPlot);
            be.stopControllingPlayer();
            return true;
         }
      } else {
         return true;
      }
   }

   @Override
   public boolean activeOnMouseMove(double yaw, double pitch) {
      double scalar = 0.5 - Math.abs(0.5 - (double)animatedValue) + 0.05;
      if (!(SableDistUtil.getClientLevel().getBlockEntity(this.getInteractionPos()) instanceof PhysicsAssemblerBlockEntity)) {
         return false;
      } else {
         animatedValue -= (float)(pitch / 80.0 * scalar);
         if ((double)animatedValue > 1.0) {
            animatedValue = 1.0F;
         } else if ((double)animatedValue < 0.0) {
            animatedValue = 0.0F;
            animatedVelocity = 0.0F;
         }

         int signal = Math.round(animatedValue * 4.0F);
         if (signal != lastSignal) {
            lastSignal = signal;
            if ((float)signal != 0.0F && (float)signal != 4.0F) {
               SimSoundEvents.ASSEMBLER_TICK.playAt(Minecraft.getInstance().level, this.getInteractionPos(), 0.5F, 0.8F + animatedValue * 0.3F, false);
            } else {
               SimSoundEvents.ASSEMBLER_SHIFT.playAt(Minecraft.getInstance().level, this.getInteractionPos(), 0.5F, 0.8F + animatedValue * 0.3F, false);
            }
         }

         return true;
      }
   }

   @Override
   public void renderOverlay(GuiGraphics graphics, int width1, int height1, boolean hideGui) {
      if (!hideGui) {
         PoseStack ps = graphics.pose();
         ps.pushPose();
         ps.translate((float)(graphics.guiWidth() / 2), (float)(graphics.guiHeight() / 2), 0.0F);
         int height = 72;
         ps.translate(10.0F, -36.0F, 0.0F);
         graphics.blit(SimGUITextures.ASSEMBLER_TRACK_START.location, 0, 0, 0.0F, 0.0F, 14, 6, 32, 32);
         ps.translate(0.0F, 6.0F, 0.0F);

         for (int c = 0; c < 6; c++) {
            graphics.blit(SimGUITextures.ASSEMBLER_TRACK_MIDDLE.location, 0, 0, 0.0F, 7.0F, 14, 10, 32, 32);
            ps.translate(0.0F, 10.0F, 0.0F);
         }

         graphics.blit(SimGUITextures.ASSEMBLER_TRACK_END.location, 0, 0, 0.0F, 18.0F, 14, 6, 32, 32);
         float value = Mth.lerp(AnimationTickHolder.getPartialTicks(), lastAnimatedValue, animatedValue);
         ps.translate(-2.0F, -12.0F - 51.0F * value, 0.0F);
         graphics.blit(SimGUITextures.ASSEMBLER_TRACK_MIDDLE.location, 0, 0, 14.0F, 0.0F, 18, 14, 32, 32);
         ps.popPose();
      }
   }

   public ClientSubLevel getSubLevelHolding() {
      return Sable.HELPER.getContainingClient(this.getInteractionPos());
   }

   public double getFraction() {
      return (double)animatedValue;
   }
}
