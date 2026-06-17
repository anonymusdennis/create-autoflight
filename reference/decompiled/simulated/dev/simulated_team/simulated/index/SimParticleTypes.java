package dev.simulated_team.simulated.index;

import com.simibubi.create.foundation.particle.ICustomParticleData;
import dev.simulated_team.simulated.content.particle.AugerIndicatorParticleData;
import dev.simulated_team.simulated.content.particle.MagnetFieldParticleData;
import dev.simulated_team.simulated.content.particle.MagnetFieldParticleData2;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public enum SimParticleTypes {
   MAGNET_FIELD(MagnetFieldParticleData::new),
   MAGNET_FIELD2(MagnetFieldParticleData2::new),
   AUGER_INDICATOR(AugerIndicatorParticleData::new);

   public final SimParticleTypes.ParticleEntry<?> entry;

   private <D extends ParticleOptions> SimParticleTypes(final Supplier<? extends ICustomParticleData<D>> typeFactory) {
      this.entry = new SimParticleTypes.ParticleEntry<>(typeFactory);
   }

   public static void register() {
   }

   public static void registerClientParticles(Consumer<SimParticleTypes.ParticleEntry<?>> consume) {
      for (SimParticleTypes value : values()) {
         consume.accept(value.entry);
      }
   }

   public ParticleType<?> get() {
      return this.entry.object;
   }

   public static class ParticleEntry<D extends ParticleOptions> {
      private final Supplier<? extends ICustomParticleData<D>> typeFactory;
      private final ParticleType<D> object;

      public ParticleEntry(Supplier<? extends ICustomParticleData<D>> typeFactory) {
         this.typeFactory = typeFactory;
         this.object = this.typeFactory.get().createType();
      }

      public Supplier<? extends ICustomParticleData<D>> getTypeFactory() {
         return this.typeFactory;
      }

      public ParticleType<D> getObject() {
         return this.object;
      }
   }
}
