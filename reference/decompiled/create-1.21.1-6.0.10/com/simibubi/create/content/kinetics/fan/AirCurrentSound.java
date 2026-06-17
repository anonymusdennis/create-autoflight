package com.simibubi.create.content.kinetics.fan;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class AirCurrentSound extends AbstractTickableSoundInstance {
   private float pitch;

   protected AirCurrentSound(SoundEvent p_i46532_1_, float pitch) {
      super(p_i46532_1_, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
      this.pitch = pitch;
      this.volume = 0.01F;
      this.looping = true;
      this.delay = 0;
      this.relative = true;
   }

   public void tick() {
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void fadeIn(float maxVolume) {
      this.volume = Math.min(maxVolume, this.volume + 0.05F);
   }

   public void fadeOut() {
      this.volume = Math.max(0.0F, this.volume - 0.05F);
   }

   public boolean isFaded() {
      return this.volume == 0.0F;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void stopSound() {
      this.stop();
   }
}
