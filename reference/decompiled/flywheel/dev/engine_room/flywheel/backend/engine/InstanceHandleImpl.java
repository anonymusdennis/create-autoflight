package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import org.jetbrains.annotations.UnknownNullability;

public class InstanceHandleImpl<I extends Instance> implements InstanceHandle {
   @UnknownNullability
   public InstanceHandleImpl.State<I> state;
   public int index;

   public InstanceHandleImpl(@UnknownNullability InstanceHandleImpl.State<I> state) {
      this.state = state;
   }

   @Override
   public void setChanged() {
      this.state = this.state.setChanged(this.index);
   }

   @Override
   public void setDeleted() {
      this.state = this.state.setDeleted(this.index);
      this.clear();
   }

   @Override
   public void setVisible(boolean visible) {
      this.state = this.state.setVisible(this, this.index, visible);
   }

   @Override
   public boolean isVisible() {
      return this.state instanceof AbstractInstancer;
   }

   public void clear() {
      this.index = -1;
   }

   public static record Deleted<I extends Instance>() implements InstanceHandleImpl.State<I> {
      private static final InstanceHandleImpl.Deleted<?> INSTANCE = new InstanceHandleImpl.Deleted();

      public static <I extends Instance> InstanceHandleImpl.Deleted<I> instance() {
         return (InstanceHandleImpl.Deleted<I>)INSTANCE;
      }

      @Override
      public InstanceHandleImpl.State<I> setChanged(int index) {
         return this;
      }

      @Override
      public InstanceHandleImpl.State<I> setDeleted(int index) {
         return this;
      }

      @Override
      public InstanceHandleImpl.State<I> setVisible(InstanceHandleImpl<I> handle, int index, boolean visible) {
         return this;
      }
   }

   public static record Hidden<I extends Instance>(AbstractInstancer.Recreate<I> recreate, I instance) implements InstanceHandleImpl.State<I> {
      @Override
      public InstanceHandleImpl.State<I> setChanged(int index) {
         return this;
      }

      @Override
      public InstanceHandleImpl.State<I> setDeleted(int index) {
         return this;
      }

      @Override
      public InstanceHandleImpl.State<I> setVisible(InstanceHandleImpl<I> handle, int index, boolean visible) {
         if (!visible) {
            return this;
         } else {
            AbstractInstancer<I> instancer = this.recreate.recreate();
            return instancer.revealInstance(handle, this.instance);
         }
      }
   }

   public interface State<I extends Instance> {
      InstanceHandleImpl.State<I> setChanged(int var1);

      InstanceHandleImpl.State<I> setDeleted(int var1);

      InstanceHandleImpl.State<I> setVisible(InstanceHandleImpl<I> var1, int var2, boolean var3);
   }
}
