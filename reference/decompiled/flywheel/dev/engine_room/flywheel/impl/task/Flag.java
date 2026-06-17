package dev.engine_room.flywheel.impl.task;

import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.Nullable;

public final class Flag {
   private final AtomicBoolean raised = new AtomicBoolean(false);
   @Nullable
   private final String name;

   public Flag(@Nullable String name) {
      this.name = name;
   }

   public Flag() {
      this(null);
   }

   public void raise() {
      this.raised.set(true);
   }

   public void lower() {
      this.raised.set(false);
   }

   public boolean isRaised() {
      return this.raised.get();
   }

   public boolean isLowered() {
      return !this.isRaised();
   }

   @Nullable
   public String name() {
      return this.name;
   }

   @Override
   public String toString() {
      return "Flag[name=" + this.name + "]";
   }
}
