package dev.engine_room.flywheel.lib.visual.util;

import dev.engine_room.flywheel.api.instance.Instance;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class SmartRecycler<K, I extends Instance> {
   private final Function<K, I> factory;
   private final Map<K, InstanceRecycler<I>> recyclers = new HashMap<>();

   public SmartRecycler(Function<K, I> factory) {
      this.factory = factory;
   }

   public void resetCount() {
      this.recyclers.values().forEach(InstanceRecycler::resetCount);
   }

   public I get(K key) {
      return this.recyclers.computeIfAbsent(key, k -> new InstanceRecycler<>(() -> this.factory.apply((K)k))).get();
   }

   public void discardExtra() {
      this.recyclers.values().forEach(InstanceRecycler::discardExtra);
   }

   public void delete() {
      this.recyclers.values().forEach(InstanceRecycler::delete);
      this.recyclers.clear();
   }
}
