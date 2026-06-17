package dev.simulated_team.simulated.content.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.particle.ICustomParticleDataWithSprite;
import dev.simulated_team.simulated.index.SimParticleTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.particle.ParticleEngine.SpriteParticleRegistration;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class MagnetFieldParticleData implements ParticleOptions, ICustomParticleDataWithSprite<MagnetFieldParticleData> {
   public static final MapCodec<MagnetFieldParticleData> CODEC = RecordCodecBuilder.mapCodec(
      i -> i.group(Codec.BOOL.fieldOf("negative").forGetter(p -> p.negative)).apply(i, MagnetFieldParticleData::new)
   );
   public static final StreamCodec<ByteBuf, MagnetFieldParticleData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, p -> p.negative, MagnetFieldParticleData::new
   );
   private boolean negative;

   public MagnetFieldParticleData(boolean negative) {
      this.negative = negative;
   }

   public MagnetFieldParticleData() {
      this.negative = false;
   }

   public ParticleType<?> getType() {
      return SimParticleTypes.MAGNET_FIELD.get();
   }

   public MapCodec<MagnetFieldParticleData> getCodec(ParticleType<MagnetFieldParticleData> type) {
      return CODEC;
   }

   public SpriteParticleRegistration<MagnetFieldParticleData> getMetaFactory() {
      return MagnetFieldParticle.Factory::new;
   }

   public StreamCodec<? super RegistryFriendlyByteBuf, MagnetFieldParticleData> getStreamCodec() {
      return STREAM_CODEC;
   }

   public boolean isNegative() {
      return this.negative;
   }

   public void setNegative(boolean negative) {
      this.negative = negative;
   }
}
