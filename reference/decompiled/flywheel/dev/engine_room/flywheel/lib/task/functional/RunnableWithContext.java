package dev.engine_room.flywheel.lib.task.functional;

import java.util.function.Consumer;

@FunctionalInterface
public interface RunnableWithContext<C> extends Consumer<C> {
   void run(C var1);

   @Override
   default void accept(C c) {
      this.run(c);
   }

   @FunctionalInterface
   public interface Ignored<C> extends RunnableWithContext<C>, Runnable {
      @Override
      void run();

      @Override
      default void run(C ignored) {
         this.run();
      }
   }
}
