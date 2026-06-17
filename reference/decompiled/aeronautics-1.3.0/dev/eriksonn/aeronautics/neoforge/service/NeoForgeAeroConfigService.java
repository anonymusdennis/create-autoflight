package dev.eriksonn.aeronautics.neoforge.service;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.infrastructure.config.CStress;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.config.client.AeroClient;
import dev.eriksonn.aeronautics.config.server.AeroServer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.createmod.catnip.config.ConfigBase;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import org.apache.commons.lang3.tuple.Pair;

public class NeoForgeAeroConfigService implements AeroConfig {
   public static final Map<Type, ConfigBase> CONFIGS = new EnumMap<>(Type.class);
   private static AeroServer server;
   private static AeroClient client;

   @Override
   public AeroServer getServerConfig() {
      return server;
   }

   @Override
   public AeroClient getClientConfig() {
      return client;
   }

   private static <T extends ConfigBase> T register(Supplier<T> factory, Type side) {
      Pair<T, ModConfigSpec> specPair = new Builder().configure(builder -> {
         T configx = factory.get();
         configx.registerAll(builder);
         return configx;
      });
      T config = (T)specPair.getLeft();
      config.specification = (ModConfigSpec)specPair.getRight();
      CONFIGS.put(side, config);
      return config;
   }

   public static void register(ModContainer container) {
      server = register(AeroServer::new, Type.SERVER);
      client = register(AeroClient::new, Type.CLIENT);

      for (Entry<Type, ConfigBase> typeConfigBaseEntry : CONFIGS.entrySet()) {
         container.registerConfig(typeConfigBaseEntry.getKey(), typeConfigBaseEntry.getValue().specification);
      }

      CStress stress = server.kinetics.stressValues;
      BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
      BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
   }
}
