package com.simibubi.create.content.contraptions.actors.seat;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ContraptionPlayerPassengerRotation {
   static boolean active;
   static int prevId;
   static float prevYaw;
   static float prevPitch;

   public static void tick() {
      active = (Boolean)AllConfigs.client().rotateWhenSeated.get();
   }

   public static void frame() {
      Player player = Minecraft.getInstance().player;
      if (active) {
         if (player != null && player.isPassenger()) {
            if (player.getVehicle() instanceof AbstractContraptionEntity contraptionEntity) {
               AbstractContraptionEntity.ContraptionRotationState rotationState = contraptionEntity.getRotationState();
               float yaw = AngleHelper.wrapAngle180(
                  contraptionEntity instanceof CarriageContraptionEntity cce ? cce.getViewYRot(AnimationTickHolder.getPartialTicks()) : rotationState.yRotation
               );
               float pitch = contraptionEntity instanceof CarriageContraptionEntity ccex ? ccex.getViewXRot(AnimationTickHolder.getPartialTicks()) : 0.0F;
               if (prevId != contraptionEntity.getId()) {
                  prevId = contraptionEntity.getId();
                  prevYaw = yaw;
                  prevPitch = pitch;
               }

               float yawDiff = AngleHelper.getShortestAngleDiff((double)yaw, (double)prevYaw);
               float pitchDiff = AngleHelper.getShortestAngleDiff((double)pitch, (double)prevPitch);
               prevYaw = yaw;
               prevPitch = pitch;
               float yawRelativeToTrain = Mth.abs(AngleHelper.getShortestAngleDiff((double)player.getYRot(), (double)(-yaw - 90.0F)));
               if (yawRelativeToTrain > 120.0F) {
                  pitchDiff *= -1.0F;
               } else if (yawRelativeToTrain > 60.0F) {
                  pitchDiff *= 0.0F;
               }

               player.setYRot(player.getYRot() + yawDiff);
               player.setXRot(player.getXRot() + pitchDiff);
            }
         } else {
            prevId = 0;
         }
      }
   }
}
