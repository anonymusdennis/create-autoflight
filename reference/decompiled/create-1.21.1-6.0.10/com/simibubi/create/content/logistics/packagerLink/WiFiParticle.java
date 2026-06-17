package com.simibubi.create.content.logistics.packagerLink;

import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.content.equipment.bell.BasicParticleData;
import com.simibubi.create.content.equipment.bell.CustomRotationParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import org.joml.Quaternionf;

public class WiFiParticle extends CustomRotationParticle {
   private SpriteSet animatedSprite;
   private boolean downward;

   public WiFiParticle(ClientLevel worldIn, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
      super(worldIn, x, y + (double)(vy < 0.0 ? -1 : 1), z, spriteSet, 0.0F);
      this.animatedSprite = spriteSet;
      this.quadSize = 0.5F;
      this.setSize(this.quadSize, this.quadSize);
      this.loopLength = 16;
      this.lifetime = 16;
      this.setSpriteFromAge(spriteSet);
      this.stoppedByCollision = true;
      this.downward = vy < 0.0;
   }

   public void tick() {
      this.setSpriteFromAge(this.animatedSprite);
      if (this.age++ >= this.lifetime) {
         this.remove();
      }
   }

   @Override
   public Quaternionf getCustomRotation(Camera camera, float partialTicks) {
      return new Quaternionf().rotateY(-camera.getYRot() * (float) (Math.PI / 180.0)).mul(new Quaternionf().rotateZ(this.downward ? (float) Math.PI : 0.0F));
   }

   public static class Data extends BasicParticleData<WiFiParticle> implements ParticleOptions {
      @Override
      public BasicParticleData.IBasicParticleFactory<WiFiParticle> getBasicFactory() {
         return WiFiParticle::new;
      }

      public ParticleType<?> getType() {
         return AllParticleTypes.WIFI.get();
      }
   }
}
