package dev.simulated_team.simulated.neoforge.service;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.infrastructure.config.CStress;
import dev.simulated_team.simulated.config.client.SimClient;
import dev.simulated_team.simulated.config.server.SimServer;
import dev.simulated_team.simulated.service.SimConfigService;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.createmod.catnip.config.ConfigBase;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import org.apache.commons.lang3.tuple.Pair;

public class NeoForgeSimConfigService implements SimConfigService {
   public static final Map<Type, ConfigBase> CONFIGS = new EnumMap<>(Type.class);
   private static SimServer server;
   private static SimClient client;

   @Override
   public boolean serverLoaded() {
      return server != null && server.specification != null && server.specification.isLoaded();
   }

   @Override
   public boolean clientLoaded() {
      return client != null && client.specification != null && client.specification.isLoaded();
   }

   @Override
   public SimServer server() {
      return server;
   }

   @Override
   public SimClient client() {
      return client;
   }

   public static ConfigBase byType(Type type) {
      return CONFIGS.get(type);
   }

   public static void registerCommon() {
      server = register(SimServer::new, Type.SERVER);
      client = register(SimClient::new, Type.CLIENT);
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

   public static void register(ModLoadingContext context, ModContainer container) {
      server = register(SimServer::new, Type.SERVER);
      client = register(SimClient::new, Type.CLIENT);

      for (Entry<Type, ConfigBase> typeConfigBaseEntry : CONFIGS.entrySet()) {
         container.registerConfig(typeConfigBaseEntry.getKey(), typeConfigBaseEntry.getValue().specification);
      }

      CStress stress = SimConfigService.INSTANCE.server().kinetics.stressValues;
      BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
      BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
   }
}
