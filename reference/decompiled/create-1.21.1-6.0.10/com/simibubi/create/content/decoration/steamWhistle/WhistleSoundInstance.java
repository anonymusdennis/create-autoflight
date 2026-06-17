package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.AllSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public class WhistleSoundInstance extends AbstractTickableSoundInstance {
   private boolean active;
   private int keepAlive;
   private WhistleBlock.WhistleSize size;

   public WhistleSoundInstance(WhistleBlock.WhistleSize size, BlockPos worldPosition) {
      super(
         (size == WhistleBlock.WhistleSize.SMALL
               ? AllSoundEvents.WHISTLE_HIGH
               : (size == WhistleBlock.WhistleSize.MEDIUM ? AllSoundEvents.WHISTLE_MEDIUM : AllSoundEvents.WHISTLE_LOW))
            .getMainEvent(),
         SoundSource.RECORDS,
         SoundInstance.createUnseededRandom()
      );
      this.size = size;
      this.looping = true;
      this.active = true;
      this.volume = 0.05F;
      this.delay = 0;
      this.keepAlive();
      Vec3 v = Vec3.atCenterOf(worldPosition);
      this.x = v.x;
      this.y = v.y;
      this.z = v.z;
   }

   public WhistleBlock.WhistleSize getOctave() {
      return this.size;
   }

   public void fadeOut() {
      this.active = false;
   }

   public void keepAlive() {
      this.keepAlive = 2;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void tick() {
      if (this.active) {
         this.volume = Math.min(1.0F, this.volume + 0.25F);
         this.keepAlive--;
         if (this.keepAlive == 0) {
            this.fadeOut();
         }
      } else {
         this.volume = Math.max(0.0F, this.volume - 0.25F);
         if (this.volume == 0.0F) {
            this.stop();
         }
      }
   }
}
