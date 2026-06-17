package dev.engine_room.flywheel.impl.task;

import dev.engine_room.flywheel.api.task.TaskExecutor;
import java.util.function.BooleanSupplier;

public interface TaskExecutorImpl extends TaskExecutor {
   boolean syncUntil(BooleanSupplier var1);

   boolean syncWhile(BooleanSupplier var1);

   void syncPoint();
}
