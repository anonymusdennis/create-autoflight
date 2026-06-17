package dev.engine_room.flywheel.backend.util;

public abstract class ReferenceCounted {
   private int referenceCount = 0;
   private boolean isDeleted = false;

   public int referenceCount() {
      return this.referenceCount;
   }

   public boolean isDeleted() {
      return this.isDeleted;
   }

   public void acquire() {
      if (this.isDeleted) {
         throw new IllegalStateException("Tried to acquire deleted instance of '" + this.getClass().getName() + "'!");
      } else {
         this.referenceCount++;
      }
   }

   public void release() {
      if (this.isDeleted) {
         throw new IllegalStateException("Tried to release deleted instance of '" + this.getClass().getName() + "'!");
      } else {
         int newCount = --this.referenceCount;
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
