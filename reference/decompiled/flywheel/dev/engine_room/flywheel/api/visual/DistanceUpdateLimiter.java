package dev.engine_room.flywheel.api.visual;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface DistanceUpdateLimiter {
   boolean shouldUpdate(double var1);
}
