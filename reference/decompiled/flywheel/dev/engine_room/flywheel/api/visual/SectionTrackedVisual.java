package dev.engine_room.flywheel.api.visual;

import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public sealed interface SectionTrackedVisual extends Visual permits LightUpdatedVisual, ShaderLightVisual {
   void setSectionCollector(SectionTrackedVisual.SectionCollector var1);

   @NonExtendable
   public interface SectionCollector {
      void sections(LongSet var1);
   }
}
