package dev.ryanhcode.sable.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;

public class SableDistUtil {
   public static Level getClientLevel() {
      return Minecraft.getInstance().level;
   }

   public static boolean isClientLevel(Level level) {
      return level instanceof ClientLevel;
   }
}
