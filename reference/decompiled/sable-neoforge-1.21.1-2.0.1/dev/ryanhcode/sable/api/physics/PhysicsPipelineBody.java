package dev.ryanhcode.sable.api.physics;

import dev.ryanhcode.sable.api.physics.mass.MassData;

public interface PhysicsPipelineBody {
   int NULL_RUNTIME_ID = -1;

   int getRuntimeId();

   MassData getMassTracker();

   boolean isRemoved();
}
