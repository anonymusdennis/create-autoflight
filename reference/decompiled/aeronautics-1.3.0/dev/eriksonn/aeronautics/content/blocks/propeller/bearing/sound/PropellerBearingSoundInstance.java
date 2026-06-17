package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.sound;

import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class PropellerBearingSoundInstance extends AbstractTickableSoundInstance {
   public float largeCutoff = 14.0F;
   public float largeCutoffFallOff = 3.0F;
   public PropellerBearingBlockEntity be;

   public PropellerBearingSoundInstance(PropellerBearingBlockEntity be, boolean large) {
      super(large ? AeroSoundEvents.PROPELLER_LARGE_LOOP.event() : AeroSoundEvents.PROPELLER_SMALL_LOOP.event(), SoundSource.AMBIENT, RandomSource.create());
      this.be = be;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.01F;
      BlockPos bpos = be.getBlockPos();
      this.x = (double)bpos.getX();
      this.y = (double)bpos.getY();
      this.z = (double)bpos.getZ();
      double weight = 0.0;
      double total = 0.0;
      PropellerActorBehaviour behavior = (PropellerActorBehaviour)this.be.getBehaviour(PropellerActorBehaviour.TYPE);

      for (PropellerActorBehaviour.PropellerLayer layer : behavior.getLayers()) {
         double w = layer.outerRadiusSquared() - layer.innerRadiusSquared();
         total += layer.offset() * w;
         weight += w;
      }

      if (weight > 0.0) {
         total /= weight;
         Vec3i normal = be.getBlockDirection().getNormal();
         this.x = this.x + total * (double)normal.getX();
         this.y = this.y + total * (double)normal.getY();
         this.z = this.z + total * (double)normal.getZ();
      }
   }

   public float getLayerVolume() {
      double rad = ((PropellerActorBehaviour)this.be.getBehaviour(PropellerActorBehaviour.TYPE)).radius;
      double res = 0.0;
      if (rad > (double)(this.largeCutoff + this.largeCutoffFallOff)) {
         res = 0.0;
      }

      if (rad > (double)(this.largeCutoff - this.largeCutoffFallOff)) {
         res = (double)((float)((rad - (double)this.largeCutoff + (double)this.largeCutoffFallOff) / (double)(2.0F * this.largeCutoffFallOff)));
      }

      if (rad < (double)(this.largeCutoff - this.largeCutoffFallOff)) {
         res = 1.0;
      }

      return (float)res;
   }

   public void tick() {
      double rad = ((PropellerActorBehaviour)this.be.getBehaviour(PropellerActorBehaviour.TYPE)).radius;
      if (!this.be.isRemoved() && this.be.getMovedContraption() != null) {
         float rpmFac = Mth.clamp(Math.abs(this.be.getAngularSpeed() / 64.0F), 0.0F, 1.0F);
         this.volume = (float)((double)(this.getLayerVolume() * (float)Math.pow((double)rpmFac, 2.0)) * rad);
         this.attenuation = Attenuation.LINEAR;
         this.pitch = 0.0F + 1.6F * rpmFac / Mth.clamp((float)rad / 5.0F, 1.0F, 5.0F);
      } else {
         this.stop();
      }
   }

   public boolean canStartSilent() {
      return true;
   }

   public boolean canPlaySound() {
      return true;
   }

   public void setVolume(float volume) {
      this.volume = volume;
   }
}
