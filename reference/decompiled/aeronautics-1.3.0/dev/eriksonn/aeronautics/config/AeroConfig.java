package dev.eriksonn.aeronautics.config;

import dev.eriksonn.aeronautics.config.client.AeroClient;
import dev.eriksonn.aeronautics.config.server.AeroServer;
import dev.simulated_team.simulated.service.ServiceUtil;

public interface AeroConfig {
   AeroConfig INSTANCE = (AeroConfig)ServiceUtil.load(AeroConfig.class);

   static AeroServer server() {
      return INSTANCE.getServerConfig();
   }

   static AeroClient client() {
      return INSTANCE.getClientConfig();
   }

   AeroServer getServerConfig();

   AeroClient getClientConfig();
}
