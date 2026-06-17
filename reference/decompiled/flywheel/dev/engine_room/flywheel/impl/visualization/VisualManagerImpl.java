package dev.engine_room.flywheel.impl.visualization;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualManager;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.storage.Storage;
import dev.engine_room.flywheel.impl.visualization.storage.Transaction;
import dev.engine_room.flywheel.lib.task.SimplePlan;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VisualManagerImpl<T, S extends Storage<T>> implements VisualManager<T> {
   private final Queue<Transaction<T>> queue = new ConcurrentLinkedQueue<>();
   private final S storage;

   public VisualManagerImpl(S storage) {
      this.storage = storage;
   }

   public S getStorage() {
      return this.storage;
   }

   @Override
   public int visualCount() {
      return this.getStorage().getAllVisuals().size();
   }

   @Override
   public void queueAdd(T obj) {
      if (this.getStorage().willAccept(obj)) {
         this.queue.add(Transaction.add(obj));
      }
   }

   @Override
   public void queueRemove(T obj) {
      this.queue.add(Transaction.remove(obj));
   }

   @Override
   public void queueUpdate(T obj) {
      if (this.getStorage().willAccept(obj)) {
         this.queue.add(Transaction.update(obj));
      }
   }

   public void processQueue(VisualizationContext visualizationContext, float partialTick) {
      S storage = this.getStorage();

      Transaction<T> transaction;
      while ((transaction = this.queue.poll()) != null) {
         switch (transaction.action()) {
            case ADD:
               storage.add(visualizationContext, transaction.obj(), partialTick);
               break;
            case REMOVE:
               storage.remove(transaction.obj());
               break;
            case UPDATE:
               storage.update(transaction.obj(), partialTick);
         }
      }
   }

   public Plan<DynamicVisual.Context> framePlan(VisualizationContext visualizationContext) {
      return SimplePlan.<DynamicVisual.Context>of(context -> this.processQueue(visualizationContext, context.partialTick())).then(this.storage.framePlan());
   }

   public Plan<TickableVisual.Context> tickPlan(VisualizationContext visualizationContext) {
      return SimplePlan.<TickableVisual.Context>of(context -> this.processQueue(visualizationContext, 1.0F)).then(this.storage.tickPlan());
   }

   public void onLightUpdate(long section) {
      this.getStorage().lightUpdatedVisuals().onLightUpdate(section);
   }

   public boolean areGpuLightSectionsDirty() {
      return this.getStorage().shaderLightVisuals().isDirty();
   }

   public LongSet gpuLightSections() {
      return this.getStorage().shaderLightVisuals().sections();
   }

   public void invalidate() {
      this.getStorage().invalidate();
   }
}
