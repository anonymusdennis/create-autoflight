package dev.eriksonn.aeronautics.content.particle;

import com.simibubi.create.AllTags.AllBlockTags;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class PropellerAirParticle extends SimpleAnimatedParticle {
   public static final double frictionScale = 0.2;
   public static final int lifeTime = 20;
   private boolean stoppedByCollision;
   boolean isVirtual;

   protected PropellerAirParticle(
      ClientLevel world, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprite, boolean enableCollision, boolean isVirtual
   ) {
      super(world, x, y, z, sprite, world.random.nextFloat() * 0.5F);
      this.quadSize *= 0.75F;
      this.lifetime = 20;
      this.bbWidth = this.bbHeight = 0.01F;
      this.hasPhysics = enableCollision;
      this.isVirtual = isVirtual;
      this.selectSprite(0);
      this.xo = x;
      this.yo = y;
      this.zo = z;
      this.xd = dx;
      this.yd = dy;
      this.zd = dz;
      this.setPos(x + dx, y + dy, z + dz);
      this.setAlpha(0.25F);
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
         this.selectSprite((int)Mth.clamp((float)this.age / (float)this.lifetime * 8.0F, 0.0F, 7.0F));
         double friction = 0.2 * new Vec3(this.xd, this.yd, this.zd).length();
         friction = Math.min(friction, 0.5);
         this.move(this.xd, this.yd, this.zd);
         this.xd *= 1.0 - friction;
         this.yd *= 1.0 - friction;
         this.zd *= 1.0 - friction;
      }
   }

   public void move(double pX, double pY, double pZ) {
      if (!this.stoppedByCollision) {
         double d0 = pX;
         double d1 = pY;
         double d2 = pZ;
         if (this.isVirtual
            && this.hasPhysics
            && !this.level.getBlockState(new BlockPos((int)Math.floor(this.x + pX), (int)Math.floor(this.y + pY), (int)Math.floor(this.z + pZ))).isAir()) {
            this.stoppedByCollision = true;
         }

         if (this.hasPhysics && (pX != 0.0 || pY != 0.0 || pZ != 0.0)) {
            if (!this.level
               .getBlockState(new BlockPos((int)Math.floor(this.x + pX), (int)Math.floor(this.y + pY), (int)Math.floor(this.z + pZ)))
               .is(AllBlockTags.FAN_TRANSPARENT.tag)) {
               Vec3 vec3 = Entity.collideBoundingBox(null, new Vec3(pX, pY, pZ), this.getBoundingBox(), this.level, List.of());
               pX = vec3.x;
               pY = vec3.y;
               pZ = vec3.z;
            } else {
               d0 = pX;
            }
         }

         if (pX != 0.0 || pY != 0.0 || pZ != 0.0) {
            this.setBoundingBox(this.getBoundingBox().move(pX, pY, pZ));
            this.setLocationFromBoundingbox();
         }

         if (Math.abs(d1) >= 1.0E-5F && Math.abs(pY) < 1.0E-5F) {
            this.stoppedByCollision = true;
         }

         this.onGround = d1 != pY && d1 < 0.0;
         if (d0 != pX) {
            this.xd = 0.0;
         }

         if (d2 != pZ) {
            this.zd = 0.0;
         }
      }
   }

   public int getLightColor(float partialTick) {
      BlockPos blockpos = new BlockPos((int)this.x, (int)this.y, (int)this.z);
      return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
   }

   private void selectSprite(int index) {
      this.setSprite(this.sprites.get(index, 8));
   }

   public static class Factory implements ParticleProvider<PropellerAirParticleData> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet animatedSprite) {
         this.spriteSet = animatedSprite;
      }

      public Particle createParticle(
         PropellerAirParticleData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed
      ) {
         return new PropellerAirParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet, data.enableCollision, data.isVirtual);
      }
   }
}
