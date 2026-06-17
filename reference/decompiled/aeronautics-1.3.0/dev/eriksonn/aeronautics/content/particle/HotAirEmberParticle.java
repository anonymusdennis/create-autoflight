package dev.eriksonn.aeronautics.content.particle;

import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

public class HotAirEmberParticle extends TextureSheetParticle implements ParticleSubLevelKickable {
   private final boolean isSoul;

   protected HotAirEmberParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, boolean isSoul) {
      super(level, x, y, z, xSpeed, ySpeed, zSpeed);
      this.isSoul = isSoul;
      this.quadSize = 0.03125F;
      this.lifetime = 18;
      float randomStrength = 0.1F;
      this.xd = (Math.random() * 2.0 - 1.0) * 0.1F;
      this.yd = (Math.random() * 2.0 - 1.0) * 0.1F;
      this.zd = (Math.random() * 2.0 - 1.0) * 0.1F;
      double d0 = (Math.random() + Math.random() + 1.0) * 0.15F;
      double d1 = Math.sqrt(this.xd * this.xd + this.yd * this.yd + this.zd * this.zd);
      this.xd = this.xd / d1 * d0 * 0.1F;
      this.yd = this.yd / d1 * d0 * 0.1F + 0.1F;
      this.zd = this.zd / d1 * d0 * 0.1F;
      this.xd *= xSpeed;
      this.yd *= ySpeed;
      this.zd *= zSpeed;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void tick() {
      super.tick();
      int fadeOutTicks = 2;
      this.alpha = 1.0F - (float)Mth.clamp(this.age - (this.lifetime - 2), 0, 1) / 2.0F;
   }

   public int getLightColor(float partialTick) {
      BlockPos blockpos = new BlockPos((int)this.x, (int)this.y, (int)this.z);
      return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) | 240 : 0;
   }

   public int getPalettePosition() {
      return Mth.floor((1.0F - (float)this.age / (float)this.lifetime) * 7.0F);
   }

   protected float getU0() {
      return super.getU0() + (float)this.getPalettePosition() / 7.0F * this.getSpriteWidth();
   }

   protected float getU1() {
      return super.getU0() + this.getSpriteWidth() / 7.0F + (float)this.getPalettePosition() / 7.0F * this.getSpriteWidth();
   }

   private float getSpriteWidth() {
      return super.getU1() - super.getU0();
   }

   protected float getV0() {
      return super.getV0() + this.getSpritePixelHeight() * (this.isSoul ? 1.0F : 0.0F);
   }

   protected float getV1() {
      return super.getV0() + this.getSpritePixelHeight() * (this.isSoul ? 2.0F : 1.0F);
   }

   private float getSpritePixelHeight() {
      return (super.getV1() - super.getV0()) / 2.0F;
   }

   public boolean sable$shouldKickFromTracking() {
      return false;
   }

   public boolean sable$shouldCollideWithTrackingSubLevel() {
      return false;
   }

   public static class Factory implements ParticleProvider<HotAirEmberParticleData> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet animatedSprite) {
         this.spriteSet = animatedSprite;
      }

      public Particle createParticle(
         HotAirEmberParticleData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
      ) {
         HotAirEmberParticle particle = new HotAirEmberParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, data.isSoul);
         particle.setSprite(this.spriteSet.get(0, 1));
         return particle;
      }
   }
}
