package com.simibubi.create.content.kinetics.turntable;

import com.simibubi.create.AllBlocks;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class TurntableHandler {
   public static void gameRenderFrame(DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      BlockPos pos = mc.player.getOnPos();
      if (mc.gameMode != null) {
         if (AllBlocks.TURNTABLE.has(mc.level.getBlockState(pos))) {
            if (mc.player.onGround()) {
               if (!mc.isPaused()) {
                  if (mc.level.getBlockEntity(pos) instanceof TurntableBlockEntity turnTable) {
                     float tickSpeed = mc.level.tickRateManager().tickrate() / 20.0F;
                     float speed = turnTable.getSpeed() * 0.6666667F * tickSpeed * deltaTracker.getRealtimeDeltaTicks();
                     if (speed != 0.0F) {
                        Vec3 origin = VecHelper.getCenterOf(pos);
                        Vec3 offset = mc.player.position().subtract(origin);
                        if (offset.length() > 0.25) {
                           speed *= (float)Mth.clamp((0.5 - offset.length()) * 2.0, 0.0, 1.0);
                        }

                        float yRotOffset = speed * deltaTracker.getGameTimeDeltaPartialTick(false);
                        mc.player.setYRot(mc.player.getYRot() - yRotOffset);
                        mc.player.yBodyRot -= yRotOffset;
                     }
                  }
               }
            }
         }
      }
   }
}
