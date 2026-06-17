package dev.engine_room.flywheel.impl;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.engine_room.flywheel.lib.backend.SimpleBackend;
import dev.engine_room.flywheel.lib.util.ResourceUtil;
import java.util.ArrayList;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

public final class BackendManagerImpl {
   public static final Backend OFF_BACKEND = SimpleBackend.builder().engineFactory(level -> {
      throw new UnsupportedOperationException("Cannot create engine when backend is off.");
   }).supported(() -> true).register(ResourceUtil.rl("off"));
   private static Backend backend = OFF_BACKEND;

   private BackendManagerImpl() {
   }

   public static Backend currentBackend() {
      return backend;
   }

   public static boolean isBackendOn() {
      return backend != OFF_BACKEND;
   }

   private static ArrayList<Backend> backendsByPriority() {
      ArrayList<Backend> backends = new ArrayList<>(Backend.REGISTRY.getAll());
      backends.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
      return backends;
   }

   public static Backend defaultBackend() {
      ArrayList<Backend> backendsByPriority = backendsByPriority();
      if (backendsByPriority.isEmpty()) {
         FlwImpl.LOGGER.warn("No backends registered, defaulting to 'flywheel:off'");
         return OFF_BACKEND;
      } else {
         return backendsByPriority.get(0);
      }
   }

   private static void chooseBackend() {
      Backend preferred = FlwConfig.INSTANCE.backend();
      if (preferred.isSupported()) {
         backend = preferred;
      } else {
         ArrayList<Backend> backendsByPriority = backendsByPriority();
         int startIndex = backendsByPriority.indexOf(preferred) + 1;
         backend = OFF_BACKEND;

         for (int i = startIndex; i < backendsByPriority.size(); i++) {
            Backend candidate = backendsByPriority.get(i);
            if (candidate.isSupported()) {
               backend = candidate;
               break;
            }
         }

         FlwImpl.LOGGER.warn("Flywheel backend fell back from '{}' to '{}'", Backend.REGISTRY.getIdOrThrow(preferred), Backend.REGISTRY.getIdOrThrow(backend));
      }
   }

   public static String getBackendString() {
      ResourceLocation backendId = Backend.REGISTRY.getId(backend);
      return backendId == null ? "[unregistered]" : backendId.toString();
   }

   public static void init() {
   }

   public static void onEndClientResourceReload(boolean didError) {
      if (!didError) {
         chooseBackend();
         VisualizationManagerImpl.resetAll();
      }
   }

   public static void onReloadLevelRenderer(ClientLevel level) {
      chooseBackend();
      VisualizationManagerImpl.reset(level);
   }
}
