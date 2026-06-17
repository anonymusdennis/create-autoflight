package dev.eriksonn.aeronautics.util;

import dev.eriksonn.aeronautics.content.blocks.hot_air.sound.BalloonBurnerSoundInstance;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.sound.PropellerBearingSoundHolder;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.sound.PropellerBearingSoundInstance;
import dev.simulated_team.simulated.mixin_interface.sounds.SoundExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public class AeroSoundDistUtil {
   // $VF: Inserted dummy exception handlers to handle obfuscated exceptions
   @Nullable
   public static Object tickPropellerSounds(PropellerBearingBlockEntity be, @Nullable Object soundInstance) {
      SoundManager soundManager = Minecraft.getInstance().getSoundManager();
      boolean needsNewSounds = false;
      if (soundInstance instanceof PropellerBearingSoundHolder smallSound) {
         PropellerBearingSoundHolder var10000 = smallSound;

         PropellerBearingSoundInstance var7;
         label42: {
            label48: {
               try {
                  var15 = var10000.small();
               } catch (Throwable var9) {
                  var14 = var9;
                  boolean var10001 = false;
                  break label48;
               }

               var7 = var15;
               var10000 = smallSound;

               try {
                  var17 = var10000.large();
                  break label42;
               } catch (Throwable var8) {
                  var14 = var8;
                  boolean var18 = false;
               }
            }

            Throwable var10 = var14;
            throw new MatchException(var10.toString(), var10);
         }

         var7 = var17;
         if (var7.isStopped() || var7.isStopped()) {
            soundManager.stop(var7);
            soundManager.stop(var7);
            needsNewSounds = true;
         }
      }

      if (soundInstance == null) {
         needsNewSounds = true;
      }

      if (needsNewSounds) {
         PropellerBearingSoundInstance smallSound = new PropellerBearingSoundInstance(be, false);
         PropellerBearingSoundInstance largeSound = new PropellerBearingSoundInstance(be, true);
         soundManager.queueTickingSound(smallSound);
         soundManager.queueTickingSound(largeSound);
         soundInstance = new PropellerBearingSoundHolder(smallSound, largeSound);
      }

      return soundInstance;
   }

   public static void tickGlobalBurnerSound() {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      SoundManager soundManager = minecraft.getSoundManager();
      if (level != null && !minecraft.isPaused()) {
         if (BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND.canPlaySound()
            && !SoundExtension.isSoundPlaying(BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND)) {
            soundManager.queueTickingSound(BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND);
            System.out.println("Start burner");
         }

         if (BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND.canPlaySound()
            && !SoundExtension.isSoundPlaying(BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND)) {
            soundManager.queueTickingSound(BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND);
         }
      }
   }

   public static void addPosHotAirBurnerSound(BlockPos pos) {
      BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND.addPos(pos);
   }

   public static void removePosHotAirBurnerSound(BlockPos pos) {
      BalloonBurnerSoundInstance.GLOBAL_HOT_AIR_BURNER_SOUND.removePos(pos);
   }

   public static void addPosSteamVentSound(BlockPos pos) {
      BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND.addPos(pos);
   }

   public static void removePosSteamVentSound(BlockPos pos) {
      BalloonBurnerSoundInstance.GLOBAL_STEAM_VENT_AIR_BURNER_SOUND.removePos(pos);
   }
}
