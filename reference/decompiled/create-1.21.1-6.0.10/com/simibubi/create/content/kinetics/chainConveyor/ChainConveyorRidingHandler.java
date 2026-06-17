package com.simibubi.create.content.kinetics.chainConveyor;

import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public class ChainConveyorRidingHandler {
   public static BlockPos ridingChainConveyor;
   public static float chainPosition;
   public static BlockPos ridingConnection;
   public static boolean flipped;
   public static int catchingUp;

   public static void embark(BlockPos lift, float position, BlockPos connection) {
      ridingChainConveyor = lift;
      chainPosition = position;
      ridingConnection = connection;
      catchingUp = 20;
      Minecraft mc = Minecraft.getInstance();
      if (mc.level.getBlockEntity(ridingChainConveyor) instanceof ChainConveyorBlockEntity clbe) {
         flipped = clbe.getSpeed() < 0.0F;
      }

      Component component = Component.translatable("mount.onboard", new Object[]{mc.options.keyShift.getTranslatedKeyMessage()});
      mc.gui.setOverlayMessage(component, false);
      mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_HIT, 1.0F, 0.5F));
   }

   public static void clientTick() {
      if (ridingChainConveyor != null) {
         Minecraft mc = Minecraft.getInstance();
         if (!mc.isPaused()) {
            if (!mc.player.isHolding(AllTags.AllItemTags.CHAIN_RIDEABLE::matches)) {
               stopRiding();
            } else if (!mc.player.isShiftKeyDown() && mc.level.getBlockEntity(ridingChainConveyor) instanceof ChainConveyorBlockEntity clbe) {
               if (ridingConnection != null && !clbe.connections.contains(ridingConnection)) {
                  stopRiding();
               } else {
                  clbe.prepareStats();
                  float chainYOffset = 0.5F * mc.player.getScale();
                  Vec3 playerPosition = mc.player.position().add(0.0, mc.player.getBoundingBox().getYsize() + (double)chainYOffset, 0.0);
                  updateTargetPosition(mc, clbe);
                  if (mc.level.getBlockEntity(ridingChainConveyor) instanceof ChainConveyorBlockEntity var8) {
                     var8.prepareStats();
                     Vec3 targetPosition;
                     if (ridingConnection != null) {
                        ChainConveyorBlockEntity.ConnectionStats stats = var8.connectionStats.get(ridingConnection);
                        targetPosition = stats.start()
                           .add(stats.end().subtract(stats.start()).normalize().scale((double)Math.min(stats.chainLength(), chainPosition)));
                     } else {
                        targetPosition = Vec3.atBottomCenterOf(ridingChainConveyor)
                           .add(VecHelper.rotate(new Vec3(0.0, 0.25, 1.0), (double)chainPosition, Axis.Y));
                     }

                     if (catchingUp > 0) {
                        catchingUp--;
                     }

                     Vec3 diff = targetPosition.subtract(playerPosition);
                     if (catchingUp != 0 || !(diff.length() > 3.0) && !(diff.y < -1.0)) {
                        mc.player.setDeltaMovement(mc.player.getDeltaMovement().scale(0.75).add(diff.scale(0.25)));
                        if (AnimationTickHolder.getTicks() % 10 == 0) {
                           CatnipServices.NETWORK.sendToServer(new ServerboundChainConveyorRidingPacket(ridingChainConveyor, false));
                        }
                     } else {
                        stopRiding();
                     }
                  }
               }
            } else {
               stopRiding();
            }
         }
      }
   }

   private static void stopRiding() {
      if (ridingChainConveyor != null) {
         CatnipServices.NETWORK.sendToServer(new ServerboundChainConveyorRidingPacket(ridingChainConveyor, true));
      }

      ridingChainConveyor = null;
      ridingConnection = null;
      Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.CHAIN_HIT, 0.75F, 0.35F));
   }

   private static void updateTargetPosition(Minecraft mc, ChainConveyorBlockEntity clbe) {
      float serverSpeed = ServerSpeedProvider.get();
      float speed = clbe.getSpeed() / 360.0F;
      float radius = 1.5F;
      float distancePerTick = Math.abs(speed);
      float degreesPerTick = speed / ((float) Math.PI * radius) * 360.0F;
      if (ridingConnection != null) {
         ChainConveyorBlockEntity.ConnectionStats stats = clbe.connectionStats.get(ridingConnection);
         if (flipped != clbe.getSpeed() < 0.0F) {
            flipped = clbe.getSpeed() < 0.0F;
            ridingChainConveyor = clbe.getBlockPos().offset(ridingConnection);
            chainPosition = stats.chainLength() - chainPosition;
            ridingConnection = ridingConnection.multiply(-1);
         } else {
            chainPosition += serverSpeed * distancePerTick;
            chainPosition = Math.min(stats.chainLength(), chainPosition);
            if (!(chainPosition < stats.chainLength())) {
               if (mc.level.getBlockEntity(clbe.getBlockPos().offset(ridingConnection)) instanceof ChainConveyorBlockEntity clbe2) {
                  chainPosition = clbe.wrapAngle(stats.tangentAngle() + 180.0F + (float)(70 * (clbe.reversed ? -1 : 1)));
                  ridingChainConveyor = clbe2.getBlockPos();
                  ridingConnection = null;
               }
            }
         }
      } else {
         float prevChainPosition = chainPosition;
         chainPosition += serverSpeed * degreesPerTick;
         chainPosition = clbe.wrapAngle(chainPosition);
         BlockPos nearestLooking = BlockPos.ZERO;
         double bestDiff = Double.MAX_VALUE;

         for (BlockPos connection : clbe.connections) {
            double diff = Vec3.atLowerCornerOf(connection).normalize().distanceToSqr(mc.player.getLookAngle().normalize());
            if (!(diff > bestDiff)) {
               nearestLooking = connection;
               bestDiff = diff;
            }
         }

         if (nearestLooking != BlockPos.ZERO) {
            float offBranchAngle = clbe.connectionStats.get(nearestLooking).tangentAngle();
            if (clbe.loopThresholdCrossed(chainPosition, prevChainPosition, offBranchAngle)) {
               chainPosition = 0.0F;
               ridingConnection = nearestLooking;
            }
         }
      }
   }
}
