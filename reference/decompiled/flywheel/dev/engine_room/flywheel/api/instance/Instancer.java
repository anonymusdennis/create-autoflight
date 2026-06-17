package dev.engine_room.flywheel.api.instance;

import dev.engine_room.flywheel.api.backend.BackendImplemented;
import org.jetbrains.annotations.Nullable;

@BackendImplemented
public interface Instancer<I extends Instance> {
   I createInstance();

   default void createInstances(I[] arr) {
      for (int i = 0; i < arr.length; i++) {
         arr[i] = this.createInstance();
      }
   }

   void stealInstance(@Nullable I var1);
}
