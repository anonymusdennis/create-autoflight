package dev.eriksonn.aeronautics.neoforge.index;

import com.simibubi.create.foundation.utility.CreateLang;
import dev.eriksonn.aeronautics.index.AeroParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AeroParticleTypesNeoForge {
   public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, "aeronautics");

   public static void registerEventListeners(IEventBus modEventBus) {
      for (AeroParticleTypes type : AeroParticleTypes.values()) {
         String name = CreateLang.asId(type.name());
         PARTICLE_TYPES.register(name, type::get);
      }

      modEventBus.addListener(AeroParticleTypesNeoForge::registerParticleProviders);
      PARTICLE_TYPES.register(modEventBus);
   }

   public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
      AeroParticleTypes.registerClientParticles(x -> x.getTypeFactory().get().register(x.getObject(), event));
   }
}
