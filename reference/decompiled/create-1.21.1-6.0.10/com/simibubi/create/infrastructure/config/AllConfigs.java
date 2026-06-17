package com.simibubi.create.infrastructure.config;

import com.simibubi.create.api.stress.BlockStressValues;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import net.createmod.catnip.config.ConfigBase;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.fml.event.config.ModConfigEvent.Loading;
import net.neoforged.fml.event.config.ModConfigEvent.Reloading;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber
public class AllConfigs {
   private static final Map<Type, ConfigBase> CONFIGS = new EnumMap<>(Type.class);
   private static CClient client;
   private static CCommon common;
   private static CServer server;

   public static CClient client() {
      return client;
   }

   public static CCommon common() {
      return common;
   }

   public static CServer server() {
      return server;
   }

   public static ConfigBase byType(Type type) {
      return CONFIGS.get(type);
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
      client = register(CClient::new, Type.CLIENT);
      common = register(CCommon::new, Type.COMMON);
      server = register(CServer::new, Type.SERVER);

      for (Entry<Type, ConfigBase> pair : CONFIGS.entrySet()) {
         container.registerConfig(pair.getKey(), pair.getValue().specification);
      }

      CStress stress = server().kinetics.stressValues;
      BlockStressValues.IMPACTS.registerProvider(stress::getImpact);
      BlockStressValues.CAPACITIES.registerProvider(stress::getCapacity);
   }

   @SubscribeEvent
   public static void onLoad(Loading event) {
      for (ConfigBase config : CONFIGS.values()) {
         if (config.specification == event.getConfig().getSpec()) {
            config.onLoad();
         }
      }
   }

   @SubscribeEvent
   public static void onReload(Reloading event) {
      for (ConfigBase config : CONFIGS.values()) {
         if (config.specification == event.getConfig().getSpec()) {
            config.onReload();
         }
      }
   }
}
