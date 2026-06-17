package dev.eriksonn.aeronautics.content.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eriksonn.aeronautics.content.particle.LevititeSparkleParticleData;
import java.util.Optional;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

public record Levitating(Float dragFraction, Optional<ParticleOptions> particle) {
   public static final Codec<Levitating> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               Codec.FLOAT.optionalFieldOf("drag_fraction", 0.93F).forGetter(Levitating::dragFraction),
               ParticleTypes.CODEC.lenientOptionalFieldOf("particle").forGetter(Levitating::particle)
            )
            .apply(i, Levitating::new)
   );
   public static final Levitating DEFAULT = new Levitating(0.93F, Optional.empty());
   public static final Levitating END_STONE = new Levitating(0.85F, Optional.empty());
   public static final Levitating LEVITITE = new Levitating(0.93F, Optional.of(new LevititeSparkleParticleData(9424022)));
   public static final Levitating PEARLESCENT_LEVITITE = new Levitating(0.93F, Optional.of(new LevititeSparkleParticleData(15521489)));
}
