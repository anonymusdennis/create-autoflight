package dev.eriksonn.aeronautics.content.particle;

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
import net.minecraft.util.ExtraCodecs;
import org.joml.Quaternionf;

public record GustParticleData(Quaternionf orientation) implements ParticleOptions, ICustomParticleDataWithSprite<GustParticleData> {
   private static final MapCodec<GustParticleData> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(ExtraCodecs.QUATERNIONF.fieldOf("orientation").forGetter(o -> o.orientation)).apply(instance, GustParticleData::new)
   );
   private static final StreamCodec<RegistryFriendlyByteBuf, GustParticleData> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.QUATERNIONF, o -> o.orientation, GustParticleData::new
   );

   public GustParticleData() {
      this(new Quaternionf());
   }

   public SpriteParticleRegistration<GustParticleData> getMetaFactory() {
      return GustParticle.Factory::new;
   }

   public MapCodec<GustParticleData> getCodec(ParticleType<GustParticleData> type) {
      return CODEC;
   }

   public StreamCodec<? super RegistryFriendlyByteBuf, GustParticleData> getStreamCodec() {
      return STREAM_CODEC;
   }

   public ParticleType<?> getType() {
      return AeroParticleTypes.GUST.get();
   }
}
