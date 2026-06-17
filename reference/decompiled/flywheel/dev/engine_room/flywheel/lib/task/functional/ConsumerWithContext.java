package dev.engine_room.flywheel.lib.task.functional;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@FunctionalInterface
public interface ConsumerWithContext<T, C> extends BiConsumer<T, C> {
   @Override
   void accept(T var1, C var2);

   @FunctionalInterface
   public interface Ignored<T, C> extends ConsumerWithContext<T, C>, Consumer<T> {
      @Override
      void accept(T var1);

      @Override
      default void accept(T t, C ignored) {
         this.accept(t);
      }
   }
}
