package dev.simulated_team.simulated.service;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import org.jetbrains.annotations.ApiStatus.Internal;

public interface SimModCompatibilityService {
   void init();

   String getModId();

   @Internal
   static void initLoaded() {
      for (SimModCompatibilityService service : ServiceLoader.load(SimModCompatibilityService.class, SimModCompatibilityService.class.getClassLoader())) {
         try {
            if (SimPlatformService.INSTANCE.isLoaded(service.getModId())) {
               service.init();
            }
         } catch (NoClassDefFoundError | ServiceConfigurationError var3) {
         }
      }
   }
}
