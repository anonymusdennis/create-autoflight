package dev.engine_room.flywheel.lib.instance;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;

public abstract class AbstractInstance implements Instance {
   protected final InstanceType<?> type;
   protected final InstanceHandle handle;

   protected AbstractInstance(InstanceType<?> type, InstanceHandle handle) {
      this.type = type;
      this.handle = handle;
   }

   @Override
   public final InstanceType<?> type() {
      return this.type;
   }

   @Override
   public final InstanceHandle handle() {
      return this.handle;
   }

   @Override
   public final void setChanged() {
      this.handle.setChanged();
   }

   @Override
   public final void delete() {
      this.handle.setDeleted();
   }

   @Override
   public final void setVisible(boolean visible) {
      this.handle.setVisible(visible);
   }
}
