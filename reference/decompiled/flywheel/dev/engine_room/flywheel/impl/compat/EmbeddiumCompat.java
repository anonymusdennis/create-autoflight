package dev.engine_room.flywheel.impl.compat;

import dev.engine_room.flywheel.impl.FlwImpl;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import org.embeddedt.embeddium.api.ChunkDataBuiltEvent;

public final class EmbeddiumCompat {
   public static final boolean ACTIVE = CompatMod.EMBEDDIUM.isLoaded;

   private EmbeddiumCompat() {
   }

   public static void init() {
      if (ACTIVE) {
         EmbeddiumCompat.Internals.init();
      }
   }

   static {
      if (ACTIVE) {
         FlwImpl.LOGGER.debug("Detected Embeddium");
      }
   }

   private static final class Internals {
      static void init() {
         ChunkDataBuiltEvent.BUS.addListener(event -> event.getDataBuilder().removeBlockEntitiesIf(VisualizationHelper::tryAddBlockEntity));
      }
   }
}
