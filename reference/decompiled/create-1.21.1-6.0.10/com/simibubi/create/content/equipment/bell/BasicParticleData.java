package com.simibubi.create.content.equipment.bell;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class BasicParticleData<T extends Particle> implements ParticleOptions, ICustomParticleDataWithSprite<BasicParticleData<T>> {
   @Override
   public StreamCodec<? super RegistryFriendlyByteBuf, BasicParticleData<T>> getStreamCodec() {
      return StreamCodec.unit(this);
   }

   @Override
   public MapCodec<BasicParticleData<T>> getCodec(ParticleType<BasicParticleData<T>> type) {
      return MapCodec.unit(this);
   }

   @OnlyIn(Dist.CLIENT)
   public abstract BasicParticleData.IBasicParticleFactory<T> getBasicFactory();

   @OnlyIn(Dist.CLIENT)
   @Override
   public SpriteParticleRegistration<BasicParticleData<T>> getMetaFactory() {
      return animatedSprite -> (data, worldIn, x, y, z, vx, vy, vz) -> this.getBasicFactory().makeParticle(worldIn, x, y, z, vx, vy, vz, animatedSprite);
   }

   public interface IBasicParticleFactory<U extends Particle> {
      U makeParticle(ClientLevel var1, double var2, double var4, double var6, double var8, double var10, double var12, SpriteSet var14);
   }
}
