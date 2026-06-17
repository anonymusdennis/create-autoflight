package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.backend.util.AtomicBitSet;
import java.util.ArrayList;
import org.jetbrains.annotations.Nullable;

public abstract class BaseInstancer<I extends Instance> extends AbstractInstancer<I> implements InstanceHandleImpl.State<I> {
   protected final Object lock = new Object();
   protected final ArrayList<I> instances = new ArrayList<>();
   protected final ArrayList<InstanceHandleImpl<I>> handles = new ArrayList<>();
   protected final AtomicBitSet changed = new AtomicBitSet();
   protected final AtomicBitSet deleted = new AtomicBitSet();

   protected BaseInstancer(InstancerKey<I> key, AbstractInstancer.Recreate<I> recreate) {
      super(key, recreate);
   }

   @Override
   public InstanceHandleImpl.State<I> setChanged(int index) {
      this.notifyDirty(index);
      return this;
   }

   @Override
   public InstanceHandleImpl.State<I> setDeleted(int index) {
      this.notifyRemoval(index);
      return InstanceHandleImpl.Deleted.instance();
   }

   @Override
   public InstanceHandleImpl.State<I> setVisible(InstanceHandleImpl<I> handle, int index, boolean visible) {
      if (visible) {
         return this;
      } else {
         this.notifyRemoval(index);
         I instance;
         synchronized (this.lock) {
            instance = this.instances.get(index);
         }

         return new InstanceHandleImpl.Hidden<>(this.recreate, instance);
      }
   }

   @Override
   public I createInstance() {
      InstanceHandleImpl<I> handle = new InstanceHandleImpl<>(this);
      I instance = this.type.create(handle);
      synchronized (this.lock) {
         handle.index = this.instances.size();
         this.addLocked(instance, handle);
         return instance;
      }
   }

   @Override
   public InstanceHandleImpl.State<I> revealInstance(InstanceHandleImpl<I> handle, I instance) {
      synchronized (this.lock) {
         handle.index = this.instances.size();
         this.addLocked(instance, handle);
         return this;
      }
   }

   @Override
   public void stealInstance(@Nullable I instance) {
      if (instance != null) {
         if (instance.handle() instanceof InstanceHandleImpl<I> handle) {
            if (handle.state != this) {
               if (!(handle.state instanceof InstanceHandleImpl.Deleted)) {
                  if (handle.state instanceof InstanceHandleImpl.Hidden<I> hidden && this.recreate.equals(hidden.recreate())) {
                     return;
                  }

                  if (handle.state instanceof BaseInstancer<I> other) {
                     other.notifyRemoval(handle.index);
                     handle.state = this;
                     synchronized (this.lock) {
                        handle.index = this.instances.size();
                        this.addLocked(instance, handle);
                     }
                  } else if (handle.state instanceof InstanceHandleImpl.Hidden) {
                     handle.state = new InstanceHandleImpl.Hidden<>(this.recreate, instance);
                  }
               }
            }
         }
      }
   }

   private void addLocked(I instance, InstanceHandleImpl<I> handle) {
      this.instances.add(instance);
      this.handles.add(handle);
      this.setIndexChanged(handle.index);
   }

   @Override
   public int instanceCount() {
      return this.instances.size();
   }

   public void notifyDirty(int index) {
      if (index >= 0 && index < this.instanceCount()) {
         this.setIndexChanged(index);
      }
   }

   protected void setIndexChanged(int index) {
      this.changed.set(index);
   }

   public void notifyRemoval(int index) {
      if (index >= 0 && index < this.instanceCount()) {
         this.deleted.set(index);
      }
   }

   @Override
   public void clear() {
      for (InstanceHandleImpl<I> handle : this.handles) {
         if (handle.state == this) {
            handle.clear();
            handle.state = InstanceHandleImpl.Deleted.instance();
         }
      }

      this.instances.clear();
      this.handles.clear();
      this.changed.clear();
      this.deleted.clear();
   }
}
