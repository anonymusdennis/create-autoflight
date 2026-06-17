package com.simibubi.create.content.equipment.bell;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class HauntedBellBlockEntity extends AbstractBellBlockEntity {
   public static final int DISTANCE = 10;
   public static final int RECHARGE_TICKS = 65;
   public static final int EFFECT_TICKS = 20;
   public int effectTicks = 0;

   public HauntedBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public PartialModel getBellModel() {
      return AllPartialModels.HAUNTED_BELL;
   }

   @Override
   public boolean ring(Level world, BlockPos pos, Direction direction) {
      if (this.isRinging && this.ringingTicks < 65) {
         return false;
      } else {
         if (world instanceof ServerLevel serverLevel) {
            HauntedBellPulser.sendPulse(serverLevel, pos, 10, false);
         }

         this.effectTicks = 20;
         return super.ring(world, pos, direction);
      }
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putInt("EffectTicks", this.effectTicks);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.effectTicks = compound.getInt("EffectTicks");
   }

   @Override
   public void tick() {
      super.tick();
      if (this.effectTicks > 0) {
         this.effectTicks--;
         if (this.level.isClientSide) {
            RandomSource rand = this.level.getRandom();
            if (!(rand.nextFloat() > 0.25F)) {
               this.spawnParticle(rand);
               this.playSound(rand);
            }
         }
      }
   }

   protected void spawnParticle(RandomSource rand) {
      double x = (double)this.worldPosition.getX() + rand.nextDouble();
      double y = (double)this.worldPosition.getY() + 0.5;
      double z = (double)this.worldPosition.getZ() + rand.nextDouble();
      double vx = rand.nextDouble() * 0.04 - 0.02;
      double vy = 0.1;
      double vz = rand.nextDouble() * 0.04 - 0.02;
      this.level.addParticle(ParticleTypes.SOUL, x, y, z, vx, vy, vz);
   }

   protected void playSound(RandomSource rand) {
      float vol = rand.nextFloat() * 0.4F + rand.nextFloat() > 0.9F ? 0.6F : 0.0F;
      float pitch = 0.6F + rand.nextFloat() * 0.4F;
      this.level.playSound(null, this.worldPosition, (SoundEvent)SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, vol, pitch);
   }
}
