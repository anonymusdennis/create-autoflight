package dev.ryanhcode.sable.neoforge.event;

import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.neoforged.bus.api.Event;

public class ForgeSablePostPhysicsTickEvent extends Event {
   private final SubLevelPhysicsSystem physicsSystem;
   private final double timeStep;

   public ForgeSablePostPhysicsTickEvent(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      this.physicsSystem = physicsSystem;
      this.timeStep = timeStep;
   }

   public SubLevelPhysicsSystem getPhysicsSystem() {
      return this.physicsSystem;
   }

   public double getTimeStep() {
      return this.timeStep;
   }
}
