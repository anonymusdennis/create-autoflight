package net.createmod.ponder.api.element;

import java.util.function.Consumer;

public interface TrackedElement<T> extends PonderSceneElement {
   void ifPresent(Consumer<T> var1);

   default boolean isStillValid(T element) {
      return true;
   }
}
