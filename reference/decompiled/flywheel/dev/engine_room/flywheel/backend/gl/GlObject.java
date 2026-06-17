package dev.engine_room.flywheel.backend.gl;

public abstract class GlObject {
   private static final int INVALID_HANDLE = Integer.MIN_VALUE;
   private int handle = Integer.MIN_VALUE;

   protected final void handle(int handle) {
      this.handle = handle;
   }

   public final int handle() {
      this.checkHandle();
      return this.handle;
   }

   protected final void checkHandle() {
      if (this.isInvalid()) {
         throw new IllegalStateException("handle is not valid.");
      }
   }

   protected final boolean isInvalid() {
      return this.handle == Integer.MIN_VALUE;
   }

   protected final void invalidateHandle() {
      this.handle = Integer.MIN_VALUE;
   }

   public void delete() {
      if (this.isInvalid()) {
         throw new IllegalStateException("handle already deleted.");
      } else {
         this.deleteInternal(this.handle);
         this.invalidateHandle();
      }
   }

   protected abstract void deleteInternal(int var1);
}
