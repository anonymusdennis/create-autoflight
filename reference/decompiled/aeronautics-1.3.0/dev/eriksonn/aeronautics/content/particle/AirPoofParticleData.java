package dev.eriksonn.aeronautics.content.particle;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class AirPoofParticleData implements ParticleOptions, ICustomParticleDataWithSprite<AirPoofParticleData> {
   private static final MapCodec<AirPoofParticleData> CODEC = MapCodec.unit(AirPoofParticleData::new);
   private final StreamCodec<FriendlyByteBuf, AirPoofParticleData> streamCodec = StreamCodec.unit(this);

   public SpriteParticleRegistration<AirPoofParticleData> getMetaFactory() {
      return AirPoofParticle.Factory::new;
   }

   public MapCodec<AirPoofParticleData> getCodec(ParticleType<AirPoofParticleData> type) {
      return CODEC;
   }

   public StreamCodec<? super RegistryFriendlyByteBuf, AirPoofParticleData> getStreamCodec() {
      return this.streamCodec;
   }

   public ParticleType<?> getType() {
      return AeroParticleTypes.AIR_POOF.get();
   }
}
