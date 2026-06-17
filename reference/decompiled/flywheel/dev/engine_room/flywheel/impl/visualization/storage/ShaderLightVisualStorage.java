package dev.engine_room.flywheel.impl.visualization.storage;

import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import java.util.Map;

public class ShaderLightVisualStorage {
   private final Map<ShaderLightVisual, SectionTracker> trackers = new Reference2ReferenceOpenHashMap();
   private final LongSet sections = new LongOpenHashSet();
   private boolean isDirty;

   public LongSet sections() {
      if (this.isDirty) {
         this.sections.clear();

         for (SectionTracker tracker : this.trackers.values()) {
            this.sections.addAll(tracker.sections());
         }

         this.isDirty = false;
      }

      return this.sections;
   }

   public boolean isDirty() {
      return this.isDirty;
   }

   public void markDirty() {
      this.isDirty = true;
   }

   public void add(ShaderLightVisual visual, SectionTracker tracker) {
      this.trackers.put(visual, tracker);
      tracker.addListener(this::markDirty);
      if (!tracker.sections().isEmpty()) {
         this.markDirty();
      }
   }

   public void remove(ShaderLightVisual visual) {
      SectionTracker tracker = this.trackers.remove(visual);
      if (tracker != null) {
         this.markDirty();
      }
   }

   public void clear() {
      this.trackers.clear();
      this.markDirty();
   }
}
