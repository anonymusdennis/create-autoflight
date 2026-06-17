package dev.engine_room.flywheel.api.task;

import java.util.concurrent.Executor;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface TaskExecutor extends Executor {
   int threadCount();
}
