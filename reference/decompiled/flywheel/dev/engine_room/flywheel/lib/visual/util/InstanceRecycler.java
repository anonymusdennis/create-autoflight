package dev.engine_room.flywheel.lib.visual.util;

import dev.engine_room.flywheel.api.instance.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class InstanceRecycler<I extends Instance> {
   private final Supplier<I> factory;
   private final List<I> instances = new ArrayList<>();
   private int count;

   public InstanceRecycler(Supplier<I> factory) {
      this.factory = factory;
   }

   public void resetCount() {
      this.count = 0;
   }

   public I get() {
      int lastCount = this.count++;
      if (lastCount < this.instances.size()) {
         return this.instances.get(lastCount);
      } else {
         I out = this.factory.get();
         this.instances.add(out);
         return out;
      }
   }

   public void discardExtra() {
      int size = this.instances.size();
      if (this.count != size) {
         List<I> extra = this.instances.subList(this.count, size);
         extra.forEach(Instance::delete);
         extra.clear();
      }
   }

   public void delete() {
      this.instances.forEach(Instance::delete);
      this.instances.clear();
   }
}
