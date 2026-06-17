package dev.engine_room.flywheel.impl.visual;

import net.minecraft.util.Mth;

public class BandedPrimeLimiter implements DistanceUpdateLimiterImpl {
   private static final int[] DIVISOR_SEQUENCE = new int[]{1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31};
   private int tickCount = 0;

   @Override
   public void tick() {
      this.tickCount++;
   }

   @Override
   public boolean shouldUpdate(double distanceSquared) {
      return this.tickCount % this.getUpdateDivisor(distanceSquared) == 0;
   }

   protected int getUpdateDivisor(double distanceSquared) {
      int dSq = Mth.ceil(distanceSquared);
      int i = dSq / 2048;
      return DIVISOR_SEQUENCE[Mth.clamp(i, 0, DIVISOR_SEQUENCE.length - 1)];
   }
}
