package com.simibubi.create.foundation;

import java.util.function.Function;
import java.util.function.Supplier;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public interface ICapabilityProvider<T> {
   @Nullable
   T getCapability();

   static <T, C> ICapabilityProvider<T> of(Function<Runnable, BlockCapabilityCache<T, C>> cacheFactory) {
      return new ICapabilityProvider.BlockCapabilityCacheProvider<>(cacheFactory);
   }

   static <T> ICapabilityProvider<T> of(Supplier<T> supplier) {
      return new ICapabilityProvider.SupplierProvider<>(supplier);
   }

   static <T> ICapabilityProvider<T> of(T cap) {
      return new ICapabilityProvider.SimpleProvider<>(cap);
   }

   @Internal
   public static class BlockCapabilityCacheProvider<T, C> implements ICapabilityProvider<T> {
      private final BlockCapabilityCache<T, C> inner;
      private volatile boolean invalid = false;

      private BlockCapabilityCacheProvider(Function<Runnable, BlockCapabilityCache<T, C>> cacheFactory) {
         this.inner = cacheFactory == null ? null : cacheFactory.apply(() -> this.invalid = true);
      }

      @Nullable
      @Override
      public T getCapability() {
         return (T)(this.inner != null && !this.invalid ? this.inner.getCapability() : null);
      }
   }

   @Internal
   public static class SimpleProvider<T> implements ICapabilityProvider<T> {
      private final T inner;

      private SimpleProvider(T inner) {
         this.inner = inner;
      }

      @Nullable
      @Override
      public T getCapability() {
         return this.inner;
      }
   }

   public static class SupplierProvider<T> implements ICapabilityProvider<T> {
      private final Supplier<T> inner;

      private SupplierProvider(Supplier<T> inner) {
         this.inner = inner;
      }

      @Nullable
      @Override
      public T getCapability() {
         return this.inner == null ? null : this.inner.get();
      }
   }
}
