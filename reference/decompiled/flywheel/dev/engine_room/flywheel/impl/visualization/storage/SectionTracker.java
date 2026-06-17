package dev.engine_room.flywheel.impl.visualization.storage;

import dev.engine_room.flywheel.api.visual.SectionTrackedVisual;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Unmodifiable;

public class SectionTracker implements SectionTrackedVisual.SectionCollector {
   private final List<Runnable> listeners = new ArrayList<>(2);
   @Unmodifiable
   private LongSet sections = LongSet.of();

   @Unmodifiable
   public LongSet sections() {
      return this.sections;
   }

   @Override
   public void sections(LongSet sections) {
      this.sections = LongSets.unmodifiable(new LongArraySet(sections));
      this.listeners.forEach(Runnable::run);
   }

   public void addListener(Runnable listener) {
      this.listeners.add(listener);
   }
}
