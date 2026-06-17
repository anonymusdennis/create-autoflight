package dev.simulated_team.simulated.content.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class MagnetFieldParticle extends SimpleAnimatedParticle {
   private final Vec3 motion;

   protected MagnetFieldParticle(ClientLevel world, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprite, boolean negative) {
      super(world, x, y, z, sprite, world.random.nextFloat() * 0.5F);
      this.hasPhysics = false;
      this.lifetime = 5;
      this.quadSize *= 0.75F;
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.motion = new Vec3(dx, dy, dz);
      this.selectSprite(0);
      this.setAlpha(0.4F);
      if (negative) {
         this.setColor(0.7F, 0.7F, 1.0F);
      } else {
         this.setColor(1.0F, 0.7F, 0.7F);
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   private void dissipate() {
      this.remove();
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         this.selectSprite(this.age);
         this.xd = this.motion.x;
         this.yd = this.motion.y;
         this.zd = this.motion.z;
         this.move(this.xd, this.yd, this.zd);
      }
   }

   public int getLightColor(float partialTick) {
      BlockPos blockpos = new BlockPos((int)this.x, (int)this.y, (int)this.z);
      return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
   }

   private void selectSprite(int index) {
      this.setSprite(this.sprites.get(index, 8));
   }

   public static class Factory implements ParticleProvider<MagnetFieldParticleData> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet animatedSprite) {
         this.spriteSet = animatedSprite;
      }

      public Particle createParticle(MagnetFieldParticleData data, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new MagnetFieldParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet, data.isNegative());
      }
   }
}
