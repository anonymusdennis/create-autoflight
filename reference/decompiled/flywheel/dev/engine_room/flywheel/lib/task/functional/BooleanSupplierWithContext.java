package dev.engine_room.flywheel.lib.task.functional;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

@FunctionalInterface
public interface BooleanSupplierWithContext<C> extends Predicate<C> {
   boolean getAsBoolean(C var1);

   @Override
   default boolean test(C c) {
      return this.getAsBoolean(c);
   }

   @FunctionalInterface
   public interface Ignored<C> extends BooleanSupplierWithContext<C>, BooleanSupplier {
      @Override
      boolean getAsBoolean();

      @Override
      default boolean getAsBoolean(C ignored) {
         return this.getAsBoolean();
      }
   }
}
