package dev.simulated_team.simulated.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public class SimDistUtil {
   @Nullable
   public static Player getClientPlayer() {
      return Minecraft.getInstance().player;
   }

   public static float getPartialTick() {
      return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
   }

   public static HitResult getHitResult() {
      return Minecraft.getInstance().hitResult;
   }
}
