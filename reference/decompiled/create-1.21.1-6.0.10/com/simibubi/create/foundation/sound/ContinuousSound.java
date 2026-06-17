package com.simibubi.create.foundation.sound;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class ContinuousSound extends AbstractTickableSoundInstance {
   private float sharedPitch;
   private SoundScape scape;
   private float relativeVolume;

   protected ContinuousSound(SoundEvent event, SoundScape scape, float sharedPitch, float relativeVolume) {
      super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
      this.scape = scape;
      this.sharedPitch = sharedPitch;
      this.relativeVolume = relativeVolume;
      this.looping = true;
      this.delay = 0;
      this.relative = false;
   }

   public void remove() {
      this.stop();
   }

   public float getVolume() {
      return this.scape.getVolume() * this.relativeVolume;
   }

   public float getPitch() {
      return this.sharedPitch;
   }

   public double getX() {
      return this.scape.getMeanPos().x;
   }

   public double getY() {
      return this.scape.getMeanPos().y;
   }

   public double getZ() {
      return this.scape.getMeanPos().z;
   }

   public void tick() {
   }
}
