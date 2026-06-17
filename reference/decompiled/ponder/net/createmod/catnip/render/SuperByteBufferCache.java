package net.createmod.catnip.render;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class SuperByteBufferCache {
   private static final SuperByteBufferCache INSTANCE = new SuperByteBufferCache();
   protected final Map<SuperByteBufferCache.Compartment<?>, Cache<Object, SuperByteBuffer>> caches = new HashMap<>();

   public static SuperByteBufferCache getInstance() {
      return INSTANCE;
   }

   public synchronized void registerCompartment(SuperByteBufferCache.Compartment<?> compartment) {
      this.caches.put(compartment, CacheBuilder.newBuilder().removalListener(n -> ((SuperByteBuffer)n.getValue()).delete()).build());
   }

   public synchronized void registerCompartment(SuperByteBufferCache.Compartment<?> compartment, long ticksUntilExpired) {
      this.caches
         .put(
            compartment,
            CacheBuilder.newBuilder()
               .expireAfterAccess(ticksUntilExpired * 50L, TimeUnit.MILLISECONDS)
               .removalListener(n -> ((SuperByteBuffer)n.getValue()).delete())
               .build()
         );
   }

   public <T> SuperByteBuffer get(SuperByteBufferCache.Compartment<T> compartment, T key, Callable<SuperByteBuffer> callable) {
      Cache<Object, SuperByteBuffer> cache = this.caches.get(compartment);
      if (cache == null) {
         throw new IllegalArgumentException(
            "Trying to access Buffer Cache for not registered Compartment: " + compartment + " <" + key.getClass().getSimpleName() + ">"
         );
      } else {
         try {
            return (SuperByteBuffer)cache.get(key, callable);
         } catch (ExecutionException var6) {
            var6.printStackTrace();
            throw new RuntimeException("Unable to populate Buffer Cache for key: " + key + " <" + key.getClass().getSimpleName() + ">");
         }
      }
   }

   public <T> void invalidate(SuperByteBufferCache.Compartment<T> compartment, T key) {
      this.caches.get(compartment).invalidate(key);
   }

   public void invalidate(SuperByteBufferCache.Compartment<?> compartment) {
      this.caches.get(compartment).invalidateAll();
   }

   public void invalidate() {
      this.caches.forEach((compartment, cache) -> cache.invalidateAll());
   }

   public static class Compartment<T> {
   }
}
