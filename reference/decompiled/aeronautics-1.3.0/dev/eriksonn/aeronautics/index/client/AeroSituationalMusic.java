package dev.eriksonn.aeronautics.index.client;

import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.api.CustomSituationalMusic;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.api.sound.SimSoundEntry;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.Music;

public class AeroSituationalMusic {
   public static RegistryObject<CustomSituationalMusic> CLEAR = create("clear", AeroSoundEvents.MUSIC_AIRSHIP_CLEAR, AeroSituationalMusic::testClear);
   public static RegistryObject<CustomSituationalMusic> RAIN = create("rain", AeroSoundEvents.MUSIC_AIRSHIP_RAIN, AeroSituationalMusic::testRain);

   public static boolean testClear(ClientLevel level, LocalPlayer player) {
      SubLevel subLevel = ((EntityMovementExtension)player).sable$getTrackingSubLevel();
      return !level.isRaining() && !level.isThundering() && player.position().y() > 100.0 && subLevel != null && level.getRandom().nextFloat() > 0.25F;
   }

   private static boolean testRain(ClientLevel level, LocalPlayer player) {
      SubLevel subLevel = ((EntityMovementExtension)player).sable$getTrackingSubLevel();
      return level.isRaining() || level.isThundering() && player.position().y() > 100.0 && subLevel != null && level.getRandom().nextFloat() > 0.25F;
   }

   private static RegistryObject<CustomSituationalMusic> create(String id, SimSoundEntry soundEntry, CustomSituationalMusic.Condition condition) {
      return create(id, soundEntry, 3, 4, false, condition);
   }

   private static RegistryObject<CustomSituationalMusic> create(
      String id, SimSoundEntry soundEntry, int minDelayMinutes, int maxDelayMinutes, boolean replaceCurrent, CustomSituationalMusic.Condition condition
   ) {
      return AeroClientRegistries.CUSTOM_SITUATIONAL_MUSIC
         .register(
            Aeronautics.path(id),
            () -> new CustomSituationalMusic(
                  new Music(soundEntry.registryObject().asHolder(), minDelayMinutes * 60000, maxDelayMinutes * 60000, replaceCurrent), condition
               )
         );
   }

   public static void init() {
   }
}
