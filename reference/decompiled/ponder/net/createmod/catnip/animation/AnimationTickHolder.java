package net.createmod.catnip.animation;

import net.createmod.catnip.levelWrappers.WrappedClientLevel;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.createmod.ponder.mixin.accessor.TimerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.LevelAccessor;

public class AnimationTickHolder {
   private static int ticks;
   private static int pausedTicks;

   public static void reset() {
      ticks = 0;
      pausedTicks = 0;
   }

   public static void tick() {
      if (!Minecraft.getInstance().isPaused()) {
         ticks = (ticks + 1) % 1728000;
      } else {
         pausedTicks = (pausedTicks + 1) % 1728000;
      }
   }

   public static int getTicks() {
      return getTicks(false);
   }

   public static int getTicks(boolean includePaused) {
      return includePaused ? ticks + pausedTicks : ticks;
   }

   public static int getTicks(LevelAccessor level) {
      if (level instanceof WrappedClientLevel) {
         return getTicks(((WrappedClientLevel)level).getWrappedLevel());
      } else {
         return level instanceof PonderLevel ? PonderUI.ponderTicks : getTicks();
      }
   }

   public static float getPartialTicks(LevelAccessor level) {
      return level instanceof PonderLevel ? PonderUI.getPartialTicks() : getPartialTicks();
   }

   public static float getRenderTime() {
      return (float)getTicks() + getPartialTicks();
   }

   public static float getRenderTime(LevelAccessor level) {
      return (float)getTicks(level) + getPartialTicks(level);
   }

   public static float getPartialTicks() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getTimer().getGameTimeDeltaPartialTick(false);
   }

   public static float getPartialTicksUI() {
      Minecraft mc = Minecraft.getInstance();
      return mc.getTimer() instanceof TimerAccessor timerAccessor ? timerAccessor.catnip$getDeltaTickResidual() : getPartialTicks();
   }
}
