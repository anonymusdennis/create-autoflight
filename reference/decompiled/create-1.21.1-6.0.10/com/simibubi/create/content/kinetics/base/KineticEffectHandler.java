package com.simibubi.create.content.kinetics.base;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class KineticEffectHandler {
   int overStressedTime;
   float overStressedEffect;
   int particleSpawnCountdown;
   KineticBlockEntity kte;

   public KineticEffectHandler(KineticBlockEntity kte) {
      this.kte = kte;
   }

   public void tick() {
      Level world = this.kte.getLevel();
      if (world.isClientSide) {
         if (this.overStressedTime > 0 && --this.overStressedTime == 0) {
            if (this.kte.isOverStressed()) {
               this.overStressedEffect = 1.0F;
               this.spawnEffect(ParticleTypes.SMOKE, 0.2F, 5);
            } else {
               this.overStressedEffect = -1.0F;
               this.spawnEffect(ParticleTypes.CLOUD, 0.075F, 2);
            }
         }

         if (this.overStressedEffect != 0.0F) {
            this.overStressedEffect = this.overStressedEffect - this.overStressedEffect * 0.1F;
            if (Math.abs(this.overStressedEffect) < 0.0078125F) {
               this.overStressedEffect = 0.0F;
            }
         }
      } else if (this.particleSpawnCountdown > 0 && --this.particleSpawnCountdown == 0) {
         this.spawnRotationIndicators();
      }
   }

   public void queueRotationIndicators() {
      this.particleSpawnCountdown = 2;
   }

   public void spawnEffect(ParticleOptions particle, float maxMotion, int amount) {
      Level world = this.kte.getLevel();
      if (world != null) {
         if (world.isClientSide) {
            RandomSource r = world.random;

            for (int i = 0; i < amount; i++) {
               Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, r, maxMotion);
               Vec3 position = VecHelper.getCenterOf(this.kte.getBlockPos());
               world.addParticle(particle, position.x, position.y, position.z, motion.x, motion.y, motion.z);
            }
         }
      }
   }

   public void spawnRotationIndicators() {
      float speed = this.kte.getSpeed();
      if (speed != 0.0F) {
         BlockState state = this.kte.getBlockState();
         if (state.getBlock() instanceof KineticBlock kb) {
            float radius1 = kb.getParticleInitialRadius();
            float radius2 = kb.getParticleTargetRadius();
            Axis axis = kb.getRotationAxis(state);
            BlockPos pos = this.kte.getBlockPos();
            Level world = this.kte.getLevel();
            if (axis != null) {
               if (world != null) {
                  Vec3 vec = VecHelper.getCenterOf(pos);
                  IRotate.SpeedLevel speedLevel = IRotate.SpeedLevel.of(speed);
                  int color = speedLevel.getColor();
                  int particleSpeed = speedLevel.getParticleSpeed();
                  particleSpeed = (int)((float)particleSpeed * Math.signum(speed));
                  if (world instanceof ServerLevel) {
                     RotationIndicatorParticleData particleData = new RotationIndicatorParticleData(color, (float)particleSpeed, radius1, radius2, 10, axis);
                     ((ServerLevel)world).sendParticles(particleData, vec.x, vec.y, vec.z, 20, 0.0, 0.0, 0.0, 1.0);
                  }
               }
            }
         }
      }
   }

   public void triggerOverStressedEffect() {
      this.overStressedTime = this.overStressedTime == 0 ? 2 : 0;
   }
}
