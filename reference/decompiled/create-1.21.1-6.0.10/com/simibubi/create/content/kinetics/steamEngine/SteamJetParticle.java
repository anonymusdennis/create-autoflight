package com.simibubi.create.content.kinetics.steamEngine;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SteamJetParticle extends SimpleAnimatedParticle {
   private float yaw;
   private float pitch;

   protected SteamJetParticle(ClientLevel world, SteamJetParticleData data, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprite) {
      super(world, x, y, z, sprite, world.random.nextFloat() * 0.5F);
      this.xd = 0.0;
      this.yd = 0.0;
      this.zd = 0.0;
      this.gravity = 0.0F;
      this.quadSize = 0.375F;
      this.setLifetime(21);
      this.setPos(x, y, z);
      this.roll = this.oRoll = world.random.nextFloat() * (float) Math.PI;
      this.yaw = (float)Mth.atan2(dx, dz) - (float) Math.PI;
      this.pitch = (float)Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) - (float) (Math.PI / 2);
      this.setSpriteFromAge(sprite);
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public void render(VertexConsumer pBuffer, Camera pRenderInfo, float pPartialTicks) {
      Vec3 vec3 = pRenderInfo.getPosition();
      float f = (float)(this.x - vec3.x);
      float f1 = (float)(this.y - vec3.y);
      float f2 = (float)(this.z - vec3.z);
      float f3 = Mth.lerp(pPartialTicks, this.oRoll, this.roll);
      float f7 = this.getU0();
      float f8 = this.getU1();
      float f5 = this.getV0();
      float f6 = this.getV1();
      float f4 = this.getQuadSize(pPartialTicks);

      for (int i = 0; i < 4; i++) {
         Quaternionf quaternion = Axis.YP.rotation(this.yaw);
         quaternion.mul(Axis.XP.rotation(this.pitch));
         quaternion.mul(Axis.YP.rotation(f3 + (float) (Math.PI / 2) * (float)i + this.roll));
         Vector3f vector3f1 = new Vector3f(-1.0F, -1.0F, 0.0F);
         vector3f1.rotate(quaternion);
         Vector3f[] avector3f = new Vector3f[]{
            new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F), new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
         };

         for (int j = 0; j < 4; j++) {
            Vector3f vector3f = avector3f[j];
            vector3f.add(0.0F, 1.0F, 0.0F);
            vector3f.rotate(quaternion);
            vector3f.mul(f4);
            vector3f.add(f, f1, f2);
         }

         int j = this.getLightColor(pPartialTicks);
         pBuffer.addVertex(avector3f[0].x(), avector3f[0].y(), avector3f[0].z())
            .setUv(f8, f6)
            .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
            .setLight(j);
         pBuffer.addVertex(avector3f[1].x(), avector3f[1].y(), avector3f[1].z())
            .setUv(f8, f5)
            .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
            .setLight(j);
         pBuffer.addVertex(avector3f[2].x(), avector3f[2].y(), avector3f[2].z())
            .setUv(f7, f5)
            .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
            .setLight(j);
         pBuffer.addVertex(avector3f[3].x(), avector3f[3].y(), avector3f[3].z())
            .setUv(f7, f6)
            .setColor(this.rCol, this.gCol, this.bCol, this.alpha)
            .setLight(j);
      }
   }

   public int getLightColor(float partialTick) {
      BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
      return this.level.isLoaded(blockpos) ? LevelRenderer.getLightColor(this.level, blockpos) : 0;
   }

   public static class Factory implements ParticleProvider<SteamJetParticleData> {
      private final SpriteSet spriteSet;

      public Factory(SpriteSet animatedSprite) {
         this.spriteSet = animatedSprite;
      }

      public Particle createParticle(SteamJetParticleData data, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
         return new SteamJetParticle(worldIn, data, x, y, z, xSpeed, ySpeed, zSpeed, this.spriteSet);
      }
   }
}
