package dev.eriksonn.aeronautics.registry;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.index.AeroRegistries;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.resources.ResourceLocation;

public class AeroRegistrate extends SimulatedRegistrate {
   public AeroRegistrate(ResourceLocation initialSection, String modId) {
      super(initialSection, modId);
   }

   public <T extends LiftingGasType> RegistryEntry<LiftingGasType, T> liftingGasType(String name, NonNullSupplier<T> type) {
      return this.simple((CreateRegistrate)this.self(), name, AeroRegistries.Keys.LIFTING_GAS_TYPE, type);
   }
}
