package com.simibubi.create.foundation.particle;

import com.simibubi.create.Create;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class AirParticle extends SimpleAnimatedParticle {
   private float originX;
   private float originY;
   private float originZ;
   private float targetX;
   private float targetY;
   private float targetZ;
   private float drag;
   private float twirlRadius;
   private float twirlAngleOffset;
   private Axis twirlAxis;

   protected AirParticle(ClientLevel world, AirParticleData data, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprite) {
      super(world, x, y, z, sprite, world.random.nextFloat() * 0.5F);
      this.quadSize *= 0.75F;
      this.hasPhysics = false;
      this.setPos(x, y, z);
      this.originX = (float)(this.xo = x);
      this.originY = (float)(this.yo = y);
      this.originZ = (float)(this.zo = z);
      this.targetX = (float)(x + dx);
      this.targetY = (float)(y + dy);
      this.targetZ = (float)(z + dz);
      this.drag = data.drag;
      this.twirlRadius = Create.RANDOM.nextFloat() / 6.0F;
      this.twirlAngleOffset = Create.RANDOM.nextFloat() * 360.0F;
      this.twirlAxis = Create.RANDOM.nextBoolean() ? Axis.X : Axis.Z;
      double length = new Vec3(dx, dy, dz).length();
      this.lifetime = Math.min((int)(length / (double)data.speed), 60);
      this.selectSprite(7);
      this.setAlpha(0.25F);
      if (length == 0.0) {
         this.remove();
         this.setAlpha(0.0F);
      }
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float progress = (float)Math.pow((double)((float)this.age / (float)this.lifetime), (double)this.drag);
         float angle = (progress * 2.0F * 360.0F + this.twirlAngleOffset) % 360.0F;
         Vec3 twirl = VecHelper.rotate(new Vec3(0.0, (double)this.twirlRadius, 0.0), (double)angle, this.twirlAxis);
         float x = (float)((double)Mth.lerp(progress, this.originX, this.targetX) + twirl.x);
         float y = (float)((double)Mth.lerp(progress, this.originY, this.targetY) + twirl.y);
         float z = (float)((double)Mth.lerp(progress, this.originZ, this.targetZ) + twirl.z);
         this.xd = (double)x - this.x;
         this.yd = (double)y - this.y;
         this.zd = (double)z - this.z;
         this.setSpriteFromAge(this.sprites);
         this.move(this.xd, this.yd, this.zd);
      }
   }

   public int getLightColor(float partialTick) {
      BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
      return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
   }

   private void selectSprite(int index) {
      this.setSprite(this.sprites.get(index, 8));
   }

   public static class Factory implements ParticleProvider<AirParticleData> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet animatedSprite) {
         this.spriteSet = animatedSprite;
      }

      public Particle createParticle(AirParticleData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new AirParticle(worldIn, data, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
      }
   }
}
