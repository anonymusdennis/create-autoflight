package dev.simulated_team.simulated.index.sounds;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

public class SimLazySoundType extends SoundType {
   private final SimLazySoundType.LazySupplier<SoundEvent> lazyBreak;
   private final SimLazySoundType.LazySupplier<SoundEvent> lazyStep;
   private final SimLazySoundType.LazySupplier<SoundEvent> lazyPlace;
   private final SimLazySoundType.LazySupplier<SoundEvent> lazyHit;
   private final SimLazySoundType.LazySupplier<SoundEvent> lazyFall;

   public SimLazySoundType(
      float volume,
      float pitch,
      Supplier<SoundEvent> lazyBreak,
      Supplier<SoundEvent> lazyStep,
      Supplier<SoundEvent> lazyPlace,
      Supplier<SoundEvent> lazyHit,
      Supplier<SoundEvent> lazyFall
   ) {
      super(volume, pitch, null, null, null, null, null);
      this.lazyBreak = SimLazySoundType.LazySupplier.of(lazyBreak);
      this.lazyStep = SimLazySoundType.LazySupplier.of(lazyStep);
      this.lazyPlace = SimLazySoundType.LazySupplier.of(lazyPlace);
      this.lazyHit = SimLazySoundType.LazySupplier.of(lazyHit);
      this.lazyFall = SimLazySoundType.LazySupplier.of(lazyFall);
   }

   public SoundEvent getBreakSound() {
      return this.lazyBreak.cast();
   }

   public SoundEvent getStepSound() {
      return this.lazyStep.cast();
   }

   public SoundEvent getPlaceSound() {
      return this.lazyPlace.cast();
   }

   public SoundEvent getHitSound() {
      return this.lazyHit.cast();
   }

   public SoundEvent getFallSound() {
      return this.lazyFall.cast();
   }

   public static class LazySupplier<T> {
      T nullableLazy;
      Supplier<T> lazyGetter;

      public static <T> SimLazySoundType.LazySupplier<T> of(Supplier<T> getter) {
         return new SimLazySoundType.LazySupplier<>(getter);
      }

      public LazySupplier(Supplier<T> getter) {
         this.lazyGetter = getter;
      }

      public T cast() {
         if (this.nullableLazy == null) {
            this.nullableLazy = this.lazyGetter.get();
         }

         return this.nullableLazy;
      }
   }
}
