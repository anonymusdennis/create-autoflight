package dev.engine_room.flywheel.lib.backend;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.Engine;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

public final class SimpleBackend implements Backend {
   private final Function<LevelAccessor, Engine> engineFactory;
   private final IntSupplier priority;
   private final BooleanSupplier isSupported;

   public SimpleBackend(Function<LevelAccessor, Engine> engineFactory, IntSupplier priority, BooleanSupplier isSupported) {
      this.engineFactory = engineFactory;
      this.priority = priority;
      this.isSupported = isSupported;
   }

   public static SimpleBackend.Builder builder() {
      return new SimpleBackend.Builder();
   }

   @Override
   public Engine createEngine(LevelAccessor level) {
      return this.engineFactory.apply(level);
   }

   @Override
   public int priority() {
      return this.priority.getAsInt();
   }

   @Override
   public boolean isSupported() {
      return this.isSupported.getAsBoolean();
   }

   public static final class Builder {
      @Nullable
      private Function<LevelAccessor, Engine> engineFactory;
      private IntSupplier priority = () -> 0;
      @Nullable
      private BooleanSupplier isSupported;

      public SimpleBackend.Builder engineFactory(Function<LevelAccessor, Engine> engineFactory) {
         this.engineFactory = engineFactory;
         return this;
      }

      public SimpleBackend.Builder priority(int priority) {
         return this.priority(() -> priority);
      }

      public SimpleBackend.Builder priority(IntSupplier priority) {
         this.priority = priority;
         return this;
      }

      public SimpleBackend.Builder supported(BooleanSupplier isSupported) {
         this.isSupported = isSupported;
         return this;
      }

      public Backend register(ResourceLocation id) {
         Objects.requireNonNull(this.engineFactory);
         Objects.requireNonNull(this.isSupported);
         return Backend.REGISTRY.registerAndGet(id, new SimpleBackend(this.engineFactory, this.priority, this.isSupported));
      }
   }
}
