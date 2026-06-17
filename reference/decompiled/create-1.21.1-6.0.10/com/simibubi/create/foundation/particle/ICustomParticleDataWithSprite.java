package com.simibubi.create.foundation.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.jetbrains.annotations.NotNull;

public interface ICustomParticleDataWithSprite<T extends ParticleOptions> extends ICustomParticleData<T> {
   @Override
   default ParticleType<T> createType() {
      return new ParticleType<T>(false) {
         @NotNull
         public MapCodec<T> codec() {
            return ICustomParticleDataWithSprite.this.getCodec(this);
         }

         @NotNull
         public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
            return ICustomParticleDataWithSprite.this.getStreamCodec();
         }
      };
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   default ParticleProvider<T> getFactory() {
      throw new IllegalAccessError("This particle type uses a metaFactory!");
   }

   @OnlyIn(Dist.CLIENT)
   SpriteParticleRegistration<T> getMetaFactory();

   @OnlyIn(Dist.CLIENT)
   @Override
   default void register(ParticleType<T> type, RegisterParticleProvidersEvent event) {
      event.registerSpriteSet(type, this.getMetaFactory());
   }
}
