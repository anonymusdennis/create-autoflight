package dev.eriksonn.aeronautics.api;

import dev.eriksonn.aeronautics.index.client.AeroClientRegistries;
import java.util.Map.Entry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.Music;

public record CustomSituationalMusic(Music music, CustomSituationalMusic.Condition condition) {
   public static Music getSituationalMusic(ClientLevel level, LocalPlayer player) {
      for (Entry<ResourceKey<CustomSituationalMusic>, CustomSituationalMusic> entry : AeroClientRegistries.CUSTOM_SITUATIONAL_MUSIC
         .asVanillaRegistry()
         .entrySet()) {
         CustomSituationalMusic value = entry.getValue();
         if (value.condition().test(level, player)) {
            return value.music();
         }
      }

      return null;
   }

   @FunctionalInterface
   public interface Condition {
      boolean test(ClientLevel var1, LocalPlayer var2);
   }
}
