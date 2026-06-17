package com.simibubi.create.content.logistics.packagePort.frogport;

import com.simibubi.create.AllSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FrogportSounds {
   public void open(Level level, BlockPos pos) {
      AllSoundEvents.FROGPORT_OPEN.playAt(level, Vec3.atCenterOf(pos), 0.5F, 1.0F, false);
   }

   public void close(Level level, BlockPos pos) {
      if (this.isPlayerNear(pos)) {
         AllSoundEvents.FROGPORT_CLOSE.playAt(level, Vec3.atCenterOf(pos), 1.0F, 1.25F + level.random.nextFloat() * 0.25F, true);
      }
   }

   public void catchPackage(Level level, BlockPos pos) {
      if (this.isPlayerNear(pos)) {
         AllSoundEvents.FROGPORT_CATCH.playAt(level, Vec3.atCenterOf(pos), 1.0F, 1.0F, false);
      }
   }

   public void depositPackage(Level level, BlockPos pos) {
      if (this.isPlayerNear(pos)) {
         AllSoundEvents.FROGPORT_DEPOSIT.playAt(level, Vec3.atCenterOf(pos), 1.0F, 1.0F, false);
      }
   }

   private boolean isPlayerNear(BlockPos pos) {
      return pos.closerThan(Minecraft.getInstance().player.blockPosition(), 20.0);
   }
}
