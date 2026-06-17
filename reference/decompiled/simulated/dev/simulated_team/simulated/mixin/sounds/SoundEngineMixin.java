package dev.simulated_team.simulated.mixin.sounds;

import dev.simulated_team.simulated.mixin_interface.sounds.SoundExtension;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({SoundEngine.class})
public class SoundEngineMixin implements SoundExtension {
   @Shadow
   @Final
   private Map<SoundInstance, ChannelHandle> instanceToChannel;

   @Override
   public boolean simulated$isSoundPlaying(SoundInstance sound) {
      return this.instanceToChannel.containsKey(sound);
   }
}
