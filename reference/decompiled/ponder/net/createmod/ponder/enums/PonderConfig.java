package net.createmod.ponder.enums;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.createmod.catnip.config.ConfigBase;
import net.createmod.ponder.config.CClient;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import org.apache.commons.lang3.tuple.Pair;

public class PonderConfig {
   private static final Map<Type, ConfigBase> CONFIGS = new EnumMap<>(Type.class);
   @Nullable
   private static CClient client;

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

   public static Set<Entry<Type, ConfigBase>> registerConfigs() {
      client = register(CClient::new, Type.CLIENT);
      return CONFIGS.entrySet();
   }

   public static void onLoad(ModConfig config) {
      for (ConfigBase configBase : CONFIGS.values()) {
         if (configBase.specification == config.getSpec()) {
            configBase.onLoad();
         }
      }
   }

   public static void onReload(ModConfig config) {
      for (ConfigBase configBase : CONFIGS.values()) {
         if (configBase.specification == config.getSpec()) {
            configBase.onReload();
         }
      }
   }

   public static CClient client() {
      if (client == null) {
         throw new AssertionError("Ponder Client Config was accessed, but not registered yet!");
      } else {
         return client;
      }
   }
}
