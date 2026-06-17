package dev.engine_room.flywheel.api.visualization;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface VisualManager<T> {
   int visualCount();

   void queueAdd(T var1);

   void queueRemove(T var1);

   void queueUpdate(T var1);
}
