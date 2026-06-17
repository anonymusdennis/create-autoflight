package com.simibubi.create.content.kinetics.fan;

import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AirFlowParticle extends SimpleAnimatedParticle {
   private final IAirCurrentSource source;
   private final AirFlowParticle.Access access = new AirFlowParticle.Access();

   protected AirFlowParticle(ClientLevel world, IAirCurrentSource source, double x, double y, double z, SpriteSet sprite) {
      super(world, x, y, z, sprite, world.random.nextFloat() * 0.5F);
      this.source = source;
      this.quadSize *= 0.75F;
      this.lifetime = 40;
      this.hasPhysics = false;
      this.selectSprite(7);
      Vec3 offset = VecHelper.offsetRandomly(Vec3.ZERO, this.random, 0.25F);
      this.setPos(x + offset.x, y + offset.y, z + offset.z);
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      this.setColor(15658734);
      this.setAlpha(0.25F);
   }

   @NotNull
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
   }

   public void tick() {
      if (this.source != null && !this.source.isSourceRemoved()) {
         this.xo = this.x;
         this.yo = this.y;
         this.zo = this.z;
         if (this.age++ >= this.lifetime) {
            this.remove();
         } else {
            AirCurrent airCurrent = this.source.getAirCurrent();
            if (airCurrent == null || !airCurrent.bounds.inflate(0.25).contains(this.x, this.y, this.z)) {
               this.remove();
               return;
            }

            Vec3 directionVec = Vec3.atLowerCornerOf(airCurrent.direction.getNormal());
            Vec3 motion = directionVec.scale(0.125);
            if (!this.source.getAirCurrent().pushing) {
               motion = motion.scale(-1.0);
            }

            double distance = new Vec3(this.x, this.y, this.z).subtract(VecHelper.getCenterOf(this.source.getAirCurrentPos())).multiply(directionVec).length()
               - 0.5;
            if (distance > (double)(airCurrent.maxDistance + 1.0F) || distance < -0.25) {
               this.remove();
               return;
            }

            motion = motion.scale((double)airCurrent.maxDistance - (distance - 1.0)).scale(0.5);
            FanProcessingType type = this.getType(distance);
            if (type == null) {
               this.setColor(15658734);
               this.setAlpha(0.25F);
               this.selectSprite((int)Mth.clamp(distance / (double)airCurrent.maxDistance * 8.0 + (double)this.random.nextInt(4), 0.0, 7.0));
            } else {
               type.morphAirFlow(this.access, this.random);
               this.selectSprite(this.random.nextInt(3));
            }

            this.xd = motion.x;
            this.yd = motion.y;
            this.zd = motion.z;
            if (this.onGround) {
               this.xd *= 0.7;
               this.zd *= 0.7;
            }

            this.move(this.xd, this.yd, this.zd);
         }
      } else {
         this.remove();
      }
   }

   @Nullable
   private FanProcessingType getType(double distance) {
      return this.source.getAirCurrent() == null ? null : this.source.getAirCurrent().getTypeAt((float)distance);
   }

   public int getLightColor(float partialTick) {
      BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
      return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
   }

   private void selectSprite(int index) {
      this.setSprite(this.sprites.get(index, 8));
   }

   private class Access implements FanProcessingType.AirFlowParticleAccess {
      @Override
      public void setColor(int color) {
         AirFlowParticle.this.setColor(color);
      }

      @Override
      public void setAlpha(float alpha) {
         AirFlowParticle.this.setAlpha(alpha);
      }

      @Override
      public void spawnExtraParticle(ParticleOptions options, float speedMultiplier) {
         AirFlowParticle.this.level
            .addParticle(
               options,
               AirFlowParticle.this.x,
               AirFlowParticle.this.y,
               AirFlowParticle.this.z,
               AirFlowParticle.this.xd * (double)speedMultiplier,
               AirFlowParticle.this.yd * (double)speedMultiplier,
               AirFlowParticle.this.zd * (double)speedMultiplier
            );
      }
   }

   public static class Factory implements ParticleProvider<AirFlowParticleData> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet animatedSprite) {
         this.spriteSet = animatedSprite;
      }

      public Particle createParticle(AirFlowParticleData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         BlockEntity be = worldIn.getBlockEntity(new BlockPos(data.posX, data.posY, data.posZ));
         if (!(be instanceof IAirCurrentSource)) {
            be = null;
         }

         return new AirFlowParticle(worldIn, (IAirCurrentSource)be, x, y, z, this.spriteSet);
      }
   }
}
