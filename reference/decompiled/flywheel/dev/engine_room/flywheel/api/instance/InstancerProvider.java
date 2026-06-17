package dev.engine_room.flywheel.api.instance;

import dev.engine_room.flywheel.api.backend.BackendImplemented;
import dev.engine_room.flywheel.api.model.Model;

@BackendImplemented
public interface InstancerProvider {
   <I extends Instance> Instancer<I> instancer(InstanceType<I> var1, Model var2, int var3);

   default <I extends Instance> Instancer<I> instancer(InstanceType<I> type, Model model) {
      return this.instancer(type, model, 0);
   }
}
