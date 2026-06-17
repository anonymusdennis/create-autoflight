package com.simibubi.create.foundation.utility;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.mutable.MutableInt;

public class TickBasedCache<K, V> implements Cache<K, V> {
   private static int currentTick = 0;
   private static int currentClientTick = 0;
   private Map<K, MutableInt> timestamps = new HashMap<>();
   private ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
   private int ticksUntilTimeout;
   private boolean resetTimerOnAccess;
   private boolean clientSide;

   public static void tick() {
      currentTick++;
   }

   public static void clientTick() {
      currentClientTick++;
   }

   public TickBasedCache(int ticksUntilTimeout, boolean resetTimerOnAccess) {
      this(ticksUntilTimeout, resetTimerOnAccess, false);
   }

   public TickBasedCache(int ticksUntilTimeout, boolean resetTimerOnAccess, boolean clientSide) {
      this.ticksUntilTimeout = ticksUntilTimeout;
      this.resetTimerOnAccess = resetTimerOnAccess;
      this.clientSide = clientSide;
   }

   public V getIfPresent(Object key) {
      MutableInt timestamp = this.timestamps.get(key);
      if (timestamp == null) {
         return null;
      } else if (timestamp.intValue() < this.ticks() - this.ticksUntilTimeout) {
         this.timestamps.remove(key);
         this.map.remove(key);
         return null;
      } else {
         if (this.resetTimerOnAccess) {
            timestamp.setValue(this.ticks());
         }

         return this.map.get(key);
      }
   }

   public int ticks() {
      return this.clientSide ? currentClientTick : currentTick;
   }

   public V get(K key, Callable<? extends V> loader) throws ExecutionException {
      V ifPresent = this.getIfPresent(key);
      if (ifPresent != null) {
         return ifPresent;
      } else {
         try {
            V entry = (V)loader.call();
            this.map.put(key, entry);
            this.timestamps.put(key, this.now());
            return entry;
         } catch (Exception var5) {
            throw new ExecutionException(var5);
         }
      }
   }

   private MutableInt now() {
      return new MutableInt(this.ticks());
   }

   public ImmutableMap<K, V> getAllPresent(Iterable<? extends Object> keys) {
      this.cleanUp();
      return ImmutableMap.copyOf(this.map);
   }

   public void put(K key, V value) {
      this.map.put(key, value);
      this.timestamps.put(key, this.now());
   }

   public void putAll(Map<? extends K, ? extends V> m) {
      m.forEach(this::put);
   }

   public void invalidate(Object key) {
      this.map.remove(key);
      this.timestamps.remove(key);
   }

   public void invalidateAll(Iterable<? extends Object> keys) {
      keys.forEach(this::invalidate);
   }

   public void invalidateAll() {
      this.map.clear();
      this.timestamps.clear();
   }

   public long size() {
      this.cleanUp();
      return (long)this.timestamps.size();
   }

   public CacheStats stats() {
      return new CacheStats(0L, 0L, 0L, 0L, 0L, 0L);
   }

   public ConcurrentMap<K, V> asMap() {
      this.cleanUp();
      return this.map;
   }

   public void cleanUp() {
      Set<K> outdated = new HashSet<>();
      this.timestamps.forEach((k, v) -> {
         if (v.intValue() < this.ticks() - this.ticksUntilTimeout) {
            outdated.add((K)k);
         }

         if (this.resetTimerOnAccess) {
            v.setValue(this.ticks());
         }
      });
      outdated.forEach(this.map::remove);
      outdated.forEach(this.timestamps::remove);
   }
}
