package dev.engine_room.flywheel.impl.visualization.storage;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.LightUpdatedVisual;
import dev.engine_room.flywheel.lib.task.Distribute;
import dev.engine_room.flywheel.lib.task.Synchronizer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class LightUpdatedVisualStorage {
   private static final long NEVER_UPDATED = Long.MIN_VALUE;
   private static final long INITIAL_UPDATE_ID = -9223372036854775807L;
   private final Map<LightUpdatedVisual, LongSet> visual2Sections = new WeakHashMap<>();
   private final Long2ObjectMap<List<LightUpdatedVisualStorage.Updater>> section2Updaters = new Long2ObjectOpenHashMap();
   private final LongSet sectionsUpdatedThisFrame = new LongOpenHashSet();
   private final Queue<LightUpdatedVisualStorage.MovedVisual> movedVisuals = new ConcurrentLinkedQueue<>();
   private long updateId = -9223372036854775807L;

   public Plan<DynamicVisual.Context> plan() {
      return (taskExecutor, context, onCompletion) -> {
         this.processMoved();
         if (this.sectionsUpdatedThisFrame.isEmpty()) {
            onCompletion.run();
         } else {
            Synchronizer sync = new Synchronizer(this.sectionsUpdatedThisFrame.size(), () -> {
               this.sectionsUpdatedThisFrame.clear();
               onCompletion.run();
            });
            long updateId = this.getNextUpdateId();
            LightUpdatedVisualStorage.Updater.Context updaterContext = new LightUpdatedVisualStorage.Updater.Context(updateId, context.partialTick());
            LongIterator var8 = this.sectionsUpdatedThisFrame.iterator();

            while (var8.hasNext()) {
               long section = (Long)var8.next();
               List<LightUpdatedVisualStorage.Updater> updaters = (List<LightUpdatedVisualStorage.Updater>)this.section2Updaters.get(section);
               if (updaters != null && !updaters.isEmpty()) {
                  taskExecutor.execute(() -> Distribute.tasks(taskExecutor, updaterContext, sync, updaters, LightUpdatedVisualStorage.Updater::updateLight));
               } else {
                  sync.decrementAndEventuallyRun();
               }
            }
         }
      };
   }

   private void processMoved() {
      LightUpdatedVisualStorage.MovedVisual moved;
      while ((moved = this.movedVisuals.poll()) != null) {
         if (this.remove(moved.visual)) {
            this.addInner(moved.visual, moved.tracker);
         }
      }
   }

   private long getNextUpdateId() {
      long out = this.updateId++;
      if (this.updateId == Long.MIN_VALUE) {
         this.updateId = -9223372036854775807L;
      }

      return out;
   }

   public void add(LightUpdatedVisual visual, SectionTracker tracker) {
      LightUpdatedVisualStorage.MovedVisual moved = new LightUpdatedVisualStorage.MovedVisual(visual, tracker);
      tracker.addListener(() -> this.movedVisuals.add(moved));
      this.addInner(visual, tracker);
   }

   private void addInner(LightUpdatedVisual visual, SectionTracker tracker) {
      if (tracker.sections().isEmpty()) {
         this.visual2Sections.put(visual, LongSet.of());
      } else {
         LongSet sections = tracker.sections();
         this.visual2Sections.put(visual, sections);
         LightUpdatedVisualStorage.Updater updater = createUpdater(visual, sections.size());
         LongIterator var5 = sections.iterator();

         while (var5.hasNext()) {
            long section = (Long)var5.next();
            ((List)this.section2Updaters.computeIfAbsent(section, $ -> new ObjectArrayList())).add(updater);
         }
      }
   }

   public boolean remove(LightUpdatedVisual visual) {
      LongSet sections = this.visual2Sections.remove(visual);
      if (sections == null) {
         return false;
      } else {
         LongIterator var3 = sections.iterator();

         while (var3.hasNext()) {
            long section = (Long)var3.next();
            List<LightUpdatedVisualStorage.Updater> updaters = (List<LightUpdatedVisualStorage.Updater>)this.section2Updaters.get(section);
            if (updaters != null) {
               updaters.remove(indexOfUpdater(updaters, visual));
            }
         }

         return true;
      }
   }

   public void onLightUpdate(long section) {
      this.sectionsUpdatedThisFrame.add(section);
   }

   public void clear() {
      this.visual2Sections.clear();
      this.section2Updaters.clear();
      this.sectionsUpdatedThisFrame.clear();
      this.movedVisuals.clear();
   }

   private static int indexOfUpdater(List<LightUpdatedVisualStorage.Updater> updaters, LightUpdatedVisual visual) {
      for (int i = 0; i < updaters.size(); i++) {
         if (updaters.get(i).visual() == visual) {
            return i;
         }
      }

      return -1;
   }

   private static LightUpdatedVisualStorage.Updater createUpdater(LightUpdatedVisual visual, int sectionCount) {
      return (LightUpdatedVisualStorage.Updater)(sectionCount == 1
         ? new LightUpdatedVisualStorage.Updater.Simple(visual)
         : new LightUpdatedVisualStorage.Updater.Synced(visual, new AtomicLong(Long.MIN_VALUE)));
   }

   private static record MovedVisual(LightUpdatedVisual visual, SectionTracker tracker) {
   }

   private sealed interface Updater permits LightUpdatedVisualStorage.Updater.Simple, LightUpdatedVisualStorage.Updater.Synced {
      void updateLight(LightUpdatedVisualStorage.Updater.Context var1);

      LightUpdatedVisual visual();

      public static record Context(long updateId, float partialTick) {
      }

      public static record Simple(LightUpdatedVisual visual) implements LightUpdatedVisualStorage.Updater {
         @Override
         public void updateLight(LightUpdatedVisualStorage.Updater.Context ctx) {
            this.visual.updateLight(ctx.partialTick);
         }
      }

      public static record Synced(LightUpdatedVisual visual, AtomicLong updateId) implements LightUpdatedVisualStorage.Updater {
         @Override
         public void updateLight(LightUpdatedVisualStorage.Updater.Context ctx) {
            if (this.updateId.getAndSet(ctx.updateId) != ctx.updateId) {
               this.visual.updateLight(ctx.partialTick);
            }
         }
      }
   }
}
