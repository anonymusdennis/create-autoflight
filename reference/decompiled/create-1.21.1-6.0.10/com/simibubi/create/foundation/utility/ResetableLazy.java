package com.simibubi.create.foundation.utility;

import java.util.function.Supplier;

public class ResetableLazy<T> implements Supplier<T> {
   private final Supplier<T> supplier;
   private T value;

   public ResetableLazy(Supplier<T> supplier) {
      this.supplier = supplier;
   }

   @Override
   public T get() {
      if (this.value == null) {
         this.value = this.supplier.get();
      }

      return this.value;
   }

   public void reset() {
      this.value = null;
   }

   public static <T> ResetableLazy<T> of(Supplier<T> supplier) {
      return new ResetableLazy<>(supplier);
   }
}
