package com.simibubi.create.foundation.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.jetbrains.annotations.NotNull;

public interface ICustomParticleData<T extends ParticleOptions> {
   MapCodec<T> getCodec(ParticleType<T> var1);

   StreamCodec<? super RegistryFriendlyByteBuf, T> getStreamCodec();

   default ParticleType<T> createType() {
      return new ParticleType<T>(false) {
         @NotNull
         public MapCodec<T> codec() {
            return ICustomParticleData.this.getCodec(this);
         }

         @NotNull
         public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
            return ICustomParticleData.this.getStreamCodec();
         }
      };
   }

   @OnlyIn(Dist.CLIENT)
   ParticleProvider<T> getFactory();

   @OnlyIn(Dist.CLIENT)
   default void register(ParticleType<T> type, RegisterParticleProvidersEvent event) {
      event.registerSpecial(type, this.getFactory());
   }
}
