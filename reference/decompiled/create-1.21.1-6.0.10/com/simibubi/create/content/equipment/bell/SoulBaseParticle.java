package com.simibubi.create.content.equipment.bell;

import com.mojang.math.Axis;
import com.simibubi.create.AllParticleTypes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import org.joml.Quaternionf;

public class SoulBaseParticle extends CustomRotationParticle {
   private final SpriteSet animatedSprite;

   public SoulBaseParticle(ClientLevel worldIn, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet) {
      super(worldIn, x, y, z, spriteSet, 0.0F);
      this.animatedSprite = spriteSet;
      this.quadSize = 0.5F;
      this.setSize(this.quadSize, this.quadSize);
      this.loopLength = 16 + (int)(this.random.nextFloat() * 2.0F - 1.0F);
      this.lifetime = (int)(90.0F / (this.random.nextFloat() * 0.36F + 0.64F));
      this.selectSpriteLoopingWithAge(this.animatedSprite);
      this.stoppedByCollision = true;
   }

   public void tick() {
      this.selectSpriteLoopingWithAge(this.animatedSprite);
      BlockPos pos = BlockPos.containing(this.x, this.y, this.z);
      if (this.age++ >= this.lifetime || !SoulPulseEffect.isDark(this.level, pos)) {
         this.remove();
      }
   }

   @Override
   public Quaternionf getCustomRotation(Camera camera, float partialTicks) {
      return Axis.XP.rotationDegrees(90.0F);
   }

   public static class Data extends BasicParticleData<SoulBaseParticle> {
      @Override
      public BasicParticleData.IBasicParticleFactory<SoulBaseParticle> getBasicFactory() {
         return SoulBaseParticle::new;
      }

      public ParticleType<?> getType() {
         return AllParticleTypes.SOUL_BASE.get();
      }
   }
}
