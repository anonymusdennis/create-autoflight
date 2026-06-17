package dev.ryanhcode.sable.api.event;

import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

@FunctionalInterface
public interface SablePostPhysicsTickEvent {
   void postPhysicsTick(SubLevelPhysicsSystem var1, double var2);
}
