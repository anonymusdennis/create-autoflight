package dev.simulated_team.simulated.index;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import foundry.veil.platform.registry.RegistrationProvider;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class SimRegistries {
   public static final Registry<NavigationTarget> NAVIGATION_TARGET = registry(SimRegistries.Keys.NAVIGATION_TARGET);
   public static final Registry<BlockPropertiesTooltip.Entry> PROPERTY_TOOLTIP = registry(SimRegistries.Keys.PROPERTY_TOOLTIP);

   private static <T> Registry<T> registry(ResourceKey<Registry<T>> registryKey) {
      RegistrationProvider<T> provider = RegistrationProvider.get(registryKey, "simulated");
      return provider.asVanillaRegistry();
   }

   public static void register() {
   }

   public static class Keys {
      public static final ResourceKey<Registry<NavigationTarget>> NAVIGATION_TARGET = key("navigation_target");
      public static final ResourceKey<Registry<BlockPropertiesTooltip.Entry>> PROPERTY_TOOLTIP = key("property_tooltip");

      private static <T> ResourceKey<Registry<T>> key(String name) {
         return ResourceKey.createRegistryKey(Simulated.path(name));
      }
   }
}
