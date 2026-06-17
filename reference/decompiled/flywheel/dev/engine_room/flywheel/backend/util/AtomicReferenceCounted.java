package dev.engine_room.flywheel.backend.util;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AtomicReferenceCounted {
   private final AtomicInteger referenceCount = new AtomicInteger(0);
   private volatile boolean isDeleted = false;

   public int referenceCount() {
      return this.referenceCount.get();
   }

   public boolean isDeleted() {
      return this.isDeleted;
   }

   public void acquire() {
      if (this.isDeleted) {
         throw new IllegalStateException("Tried to acquire deleted instance of '" + this.getClass().getName() + "'!");
      } else {
         this.referenceCount.getAndIncrement();
      }
   }

   public void release() {
      if (this.isDeleted) {
         throw new IllegalStateException("Tried to release deleted instance of '" + this.getClass().getName() + "'!");
      } else {
         int newCount = this.referenceCount.decrementAndGet();
         if (newCount == 0) {
            this.isDeleted = true;
            this._delete();
         } else if (newCount < 0) {
            throw new IllegalStateException("Tried to delete instance of '" + this.getClass().getName() + "' more times than it was acquired!");
         }
      }
   }

   protected abstract void _delete();
}
