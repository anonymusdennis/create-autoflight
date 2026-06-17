package dev.engine_room.flywheel.impl;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.backend.BackendConfig;
import dev.engine_room.flywheel.backend.compile.LightSmoothness;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class NeoForgeFlwConfig implements FlwConfig {
   public static final NeoForgeFlwConfig INSTANCE = new NeoForgeFlwConfig();
   public final NeoForgeFlwConfig.ClientConfig client;
   private final ModConfigSpec clientSpec;

   private NeoForgeFlwConfig() {
      Pair<NeoForgeFlwConfig.ClientConfig, ModConfigSpec> clientPair = new Builder().configure(NeoForgeFlwConfig.ClientConfig::new);
      this.client = (NeoForgeFlwConfig.ClientConfig)clientPair.getLeft();
      this.clientSpec = (ModConfigSpec)clientPair.getRight();
   }

   @Override
   public Backend backend() {
      Backend backend = parseBackend((String)this.client.backend.get());
      if (backend == null) {
         this.client.backend.set("DEFAULT");
         backend = BackendManager.defaultBackend();
      }

      return backend;
   }

   @Nullable
   private static Backend parseBackend(String value) {
      if (value.equals("DEFAULT")) {
         return BackendManager.defaultBackend();
      } else {
         ResourceLocation backendId;
         try {
            backendId = ResourceLocation.parse(value);
         } catch (ResourceLocationException var3) {
            FlwImpl.CONFIG_LOGGER.warn("'backend' value '{}' is not a valid resource location", value);
            return null;
         }

         Backend backend = Backend.REGISTRY.get(backendId);
         if (backend == null) {
            FlwImpl.CONFIG_LOGGER.warn("Backend with ID '{}' is not registered", backendId);
            return null;
         } else {
            return backend;
         }
      }
   }

   @Override
   public boolean limitUpdates() {
      return (Boolean)this.client.limitUpdates.get();
   }

   @Override
   public int workerThreads() {
      return (Integer)this.client.workerThreads.get();
   }

   @Override
   public BackendConfig backendConfig() {
      return this.client.backendConfig;
   }

   public void registerSpecs(ModContainer context) {
      context.registerConfig(Type.CLIENT, this.clientSpec);
   }

   public static class ClientConfig {
      public final ConfigValue<String> backend;
      public final BooleanValue limitUpdates;
      public final IntValue workerThreads;
      public final NeoForgeFlwConfig.NeoForgeBackendConfig backendConfig;

      private ClientConfig(Builder builder) {
         this.backend = builder.comment("Select the backend to use. Set to \"DEFAULT\" to let Flywheel decide.").define("backend", "DEFAULT");
         this.limitUpdates = builder.comment("Enable or disable instance update limiting with distance.").define("limitUpdates", true);
         this.workerThreads = builder.comment(
               "The number of worker threads to use. Set to -1 to let Flywheel decide. Set to 0 to disable parallelism. Requires a game restart to take effect."
            )
            .defineInRange("workerThreads", -1, -1, Runtime.getRuntime().availableProcessors());
         builder.comment("Config options for Flywheel's built-in backends.").push("flw_backends");
         this.backendConfig = new NeoForgeFlwConfig.NeoForgeBackendConfig(builder);
      }
   }

   public static class NeoForgeBackendConfig implements BackendConfig {
      public final EnumValue<LightSmoothness> lightSmoothness;

      public NeoForgeBackendConfig(Builder builder) {
         this.lightSmoothness = builder.comment("How smooth Flywheel's shader-based lighting should be. May have a large performance impact.")
            .defineEnum("lightSmoothness", LightSmoothness.SMOOTH);
      }

      @Override
      public LightSmoothness lightSmoothness() {
         return (LightSmoothness)this.lightSmoothness.get();
      }
   }
}
