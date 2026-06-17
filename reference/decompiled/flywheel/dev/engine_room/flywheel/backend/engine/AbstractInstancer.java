package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.backend.engine.embed.Environment;

public abstract class AbstractInstancer<I extends Instance> implements Instancer<I> {
   public final InstanceType<I> type;
   public final Environment environment;
   public final AbstractInstancer.Recreate<I> recreate;

   protected AbstractInstancer(InstancerKey<I> key, AbstractInstancer.Recreate<I> recreate) {
      this.type = key.type();
      this.environment = key.environment();
      this.recreate = recreate;
   }

   public abstract InstanceHandleImpl.State<I> revealInstance(InstanceHandleImpl<I> var1, I var2);

   public abstract int instanceCount();

   public abstract void parallelUpdate();

   public abstract void delete();

   public abstract void clear();

   @Override
   public String toString() {
      return "AbstractInstancer[" + this.instanceCount() + "]";
   }

   public static record Recreate<I extends Instance>(InstancerKey<I> key, DrawManager<?> drawManager) {
      public AbstractInstancer<I> recreate() {
         return this.drawManager.getInstancer(this.key);
      }
   }
}
