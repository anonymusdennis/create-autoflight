package dev.eriksonn.aeronautics.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class HotAirEmberParticleData implements ParticleOptions, ICustomParticleDataWithSprite<HotAirEmberParticleData> {
   private static final MapCodec<HotAirEmberParticleData> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.BOOL.fieldOf("isSoul").forGetter(p -> p.isSoul)).apply(i, HotAirEmberParticleData::new)
   );
   private static final StreamCodec<RegistryFriendlyByteBuf, HotAirEmberParticleData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, p -> p.isSoul, HotAirEmberParticleData::new
   );
   protected final boolean isSoul;

   public HotAirEmberParticleData(boolean isSoul) {
      this.isSoul = isSoul;
   }

   public HotAirEmberParticleData() {
      this.isSoul = false;
   }

   public ParticleType<?> getType() {
      return AeroParticleTypes.HOT_AIR_EMBER.get();
   }

   public SpriteParticleRegistration<HotAirEmberParticleData> getMetaFactory() {
      return HotAirEmberParticle.Factory::new;
   }

   public MapCodec<HotAirEmberParticleData> getCodec(ParticleType<HotAirEmberParticleData> particleType) {
      return CODEC;
   }

   public StreamCodec<? super RegistryFriendlyByteBuf, HotAirEmberParticleData> getStreamCodec() {
      return STREAM_CODEC;
   }
}
