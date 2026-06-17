package dev.engine_room.flywheel.lib.task.functional;

import java.util.function.Function;
import java.util.function.Supplier;

@FunctionalInterface
public interface SupplierWithContext<C, R> extends Function<C, R> {
   R get(C var1);

   @Override
   default R apply(C c) {
      return this.get(c);
   }

   @FunctionalInterface
   public interface Ignored<C, R> extends SupplierWithContext<C, R>, Supplier<R> {
      @Override
      R get();

      @Override
      default R get(C ignored) {
         return this.get();
      }
   }
}
