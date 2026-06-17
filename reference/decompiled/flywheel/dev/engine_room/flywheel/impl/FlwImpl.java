package dev.engine_room.flywheel.impl;

import dev.engine_room.flywheel.backend.FlwBackend;
import dev.engine_room.flywheel.impl.registry.IdRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FlwImpl {
   public static final Logger LOGGER = LoggerFactory.getLogger("flywheel");
   public static final Logger CONFIG_LOGGER = LoggerFactory.getLogger("flywheel/config");

   private FlwImpl() {
   }

   public static void init() {
      BackendManagerImpl.init();
      FlwBackend.init(FlwConfig.INSTANCE.backendConfig());
   }

   public static void freezeRegistries() {
      IdRegistryImpl.freezeAll();
   }
}
