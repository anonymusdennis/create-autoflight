package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.network.packets.SteeringWheelPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.hold_interaction.BlockHoldInteraction;
import dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SteeringWheelHandler extends BlockHoldInteraction {
   private static SteeringWheelBlockEntity blockEntity = null;
   private static boolean updated = false;
   private static float rawAngle = 0.0F;
   private static float effectiveAngle = 0.0F;
   private static boolean wasShiftKeyDown = false;
   private static int angleSgn = 1;
   private static float angleLimit = 0.0F;

   @Override
   public void startHold(Level level, Player player, BlockPos blockPos) {
      super.startHold(level, player, blockPos);
      blockEntity = (SteeringWheelBlockEntity)level.getBlockEntity(blockPos, (BlockEntityType)SimBlockEntityTypes.STEERING_WHEEL.get()).orElseThrow();
      rawAngle = blockEntity.getInteractionAngle(Minecraft.getInstance().getTimer().getGameTimeDeltaTicks());
      angleSgn = (int)blockEntity.directionConvert(1.0F);
      updated = true;
      angleLimit = (float)blockEntity.angleInput.getValue();
   }

   @Override
   public void renderOverlay(GuiGraphics guiGraphics, int width1, int height1, boolean hideGui) {
      Minecraft mc = Minecraft.getInstance();
      if (!hideGui) {
         if (mc.player == null || GogglesItem.isWearingGoggles(mc.player)) {
            ResourceLocation tex = Simulated.path("textures/gui/steering_wheel.png");
            float magicOffset = 0.56F;
            int x = (width1 - 223) / 2 + (Integer)SimConfigService.INSTANCE.client().blockConfig.steeringWheelXOffset.get();
            int y = 10 + (Integer)SimConfigService.INSTANCE.client().blockConfig.steeringWheelYOffset.get();
            guiGraphics.blit(tex, x, y, 0.0F, 0.0F, 223, 31, 256, 256);
            float offset = wrapDegrees(angleLimit) * 0.56F;
            int activeWidth = (int)Math.abs(offset);
            int centerX = x + 111 - 4;
            float realDegrees = (float)angleSgn * -effectiveAngle;
            if (Math.abs(angleLimit) <= 180.0F) {
               int leftDeadZoneWidth = centerX - x - activeWidth + 4;
               if (leftDeadZoneWidth > 0) {
                  guiGraphics.blit(tex, x, y, 0.0F, 32.0F, leftDeadZoneWidth, 31, 256, 256);
               }

               int rightSideStart = centerX + activeWidth + 8;
               int rightDeadZoneWidth = x + 223 - rightSideStart;
               if (rightDeadZoneWidth > 0) {
                  guiGraphics.blit(tex, rightSideStart, y, (float)(rightSideStart - x), 32.0F, rightDeadZoneWidth, 31, 256, 256);
               }
            } else {
               if (realDegrees <= -180.0F) {
                  int rightSideStart = centerX - activeWidth + 4;
                  int rightDeadZoneWidth = x + 223 - rightSideStart;
                  if (rightDeadZoneWidth > 0) {
                     guiGraphics.blit(tex, rightSideStart, y, (float)(rightSideStart - x), 32.0F, rightDeadZoneWidth, 31, 256, 256);
                  }
               }

               if (realDegrees >= 180.0F) {
                  int leftDeadZoneWidthx = centerX - x + activeWidth + 4;
                  if (leftDeadZoneWidthx > 0) {
                     guiGraphics.blit(tex, x, y, 0.0F, 32.0F, leftDeadZoneWidthx, 31, 256, 256);
                  }
               }
            }

            if (Math.abs(angleLimit) > 180.0F) {
               if (-realDegrees >= 180.0F) {
                  guiGraphics.blit(tex, (int)((float)centerX + offset) + 2, y + 10, 239.0F, 0.0F, 6, 20, 256, 256);
               }

               if (-realDegrees <= -180.0F) {
                  guiGraphics.blit(tex, (int)((float)centerX - offset) + 2, y + 10, 239.0F, 0.0F, 6, 20, 256, 256);
               }
            } else {
               guiGraphics.blit(tex, (int)((float)centerX + offset) + 2, y + 10, 239.0F, 0.0F, 6, 20, 256, 256);
               guiGraphics.blit(tex, (int)((float)centerX - offset) + 2, y + 10, 239.0F, 0.0F, 6, 20, 256, 256);
            }

            float degrees = Math.abs(angleLimit) <= 180.0F ? Mth.clamp(realDegrees, -180.0F, 180.0F) : wrapDegrees(realDegrees);
            int markerX = (int)((float)centerX - degrees * 0.56F) + 1;
            guiGraphics.blit(tex, markerX, y + 11, 224.0F, 0.0F, 9, 18, 256, 256);
            String text = (int)(-realDegrees) + "°";
            int textWidth = mc.font.width(text);
            int centeredX = markerX + 6 - textWidth / 2;

            for (int xoff = -1; xoff < 2; xoff++) {
               for (int yoff = -1; yoff < 2; yoff++) {
                  if (xoff != 0 || yoff != 0) {
                     guiGraphics.drawString(mc.font, text, centeredX + xoff, y + yoff, (int)Long.parseLong("2b2117", 16), false);
                  }
               }
            }

            guiGraphics.drawString(mc.font, text, centeredX, y, (int)Long.parseLong("886539", 16), false);
         }
      }
   }

   public static float wrapDegrees(float value) {
      float f = value % 360.0F;
      if (f >= 180.0F) {
         f -= 360.0F;
      } else if (f <= -180.0F) {
         f += 360.0F;
      }

      return f;
   }

   @Override
   public void stop() {
      if (blockEntity != null && !blockEntity.isRemoved()) {
         blockEntity.held = false;
         blockEntity = null;
      }

      VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new SteeringWheelPacket(true, effectiveAngle, this.getInteractionPos())});
      super.stop();
   }

   @Override
   public boolean activeOnMouseMove(double yaw, double pitch) {
      if (yaw != 0.0) {
         float oldAngle = rawAngle;
         rawAngle = rawAngle + (float)(yaw / 10.0 * (double)angleSgn);
         rawAngle = Mth.clamp(rawAngle, (float)(-blockEntity.angleInput.getValue()), (float)blockEntity.angleInput.getValue());
         updated = updated | oldAngle != rawAngle;
      }

      return true;
   }

   @Override
   public boolean activeTick(Level level, LocalPlayer player) {
      effectiveAngle = rawAngle;
      if (HoldInteractionManager.unblockedShift()) {
         effectiveAngle = (float)Mth.clamp(Math.round(effectiveAngle / 45.0F) * 45, -blockEntity.angleInput.getValue(), blockEntity.angleInput.getValue());
         if (!wasShiftKeyDown) {
            updated = true;
         }

         wasShiftKeyDown = true;
      } else {
         if (wasShiftKeyDown) {
            updated = true;
         }

         wasShiftKeyDown = false;
      }

      this.setTargetAngle(effectiveAngle);
      return !BlockHoldInteraction.inInteractionRange(player, this.getInteractionPos().getCenter());
   }

   @Override
   public boolean isBlockActive(BlockPos pos) {
      return super.isBlockActive(pos) && !Float.isNaN(rawAngle);
   }

   public void setTargetAngle(float targetAngle) {
      if (updated) {
         VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new SteeringWheelPacket(false, targetAngle, this.getInteractionPos())});
         updated = false;
         blockEntity.targetAngleToUpdate = targetAngle;
         blockEntity.held = !Float.isNaN(targetAngle);
      }
   }

   @Override
   public int getCrouchBlockingTicks() {
      return 6;
   }
}
