package dev.eriksonn.aeronautics.content.particle;

import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public class AirPoofParticle extends TextureSheetParticle implements ParticleSubLevelKickable {
   protected AirPoofParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed);
      this.alpha = level.random.nextFloat() * 0.2F + 0.3F;
      this.xd = xSpeed;
      this.yd = ySpeed;
      this.zd = zSpeed;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public boolean sable$shouldKickFromTracking() {
      return false;
   }

   public boolean sable$shouldCollideWithTrackingSubLevel() {
      return false;
   }

   public static record Factory(SpriteSet spriteSet) implements ParticleProvider<AirPoofParticleData> {
      public Particle createParticle(AirPoofParticleData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         AirPoofParticle particle = new AirPoofParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
         particle.pickSprite(this.spriteSet);
         return particle;
      }
   }
}
