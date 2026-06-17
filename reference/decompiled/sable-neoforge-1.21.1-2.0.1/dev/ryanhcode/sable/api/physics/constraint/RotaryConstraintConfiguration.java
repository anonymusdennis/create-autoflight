package dev.ryanhcode.sable.api.physics.constraint;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import org.joml.Vector3dc;

public record RotaryConstraintConfiguration(Vector3dc pos1, Vector3dc pos2, Vector3dc normal1, Vector3dc normal2)
   implements PhysicsConstraintConfiguration<RotaryConstraintHandle> {
   private static final double NORMAL_LENGTH_SANITY_CHECK = 1.2100000000000002;

   @Override
   public void validate(ServerSubLevelContainer container, PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB) {
      PhysicsConstraintConfiguration.validateAnchors(container, bodyA, bodyB, this.pos1, this.pos2);
      if (this.normal1.lengthSquared() > 1.2100000000000002) {
         throw new IllegalArgumentException("The first normal in this constraint should be normalized: " + this.normal1);
      } else if (this.normal2.lengthSquared() > 1.2100000000000002) {
         throw new IllegalArgumentException("The second normal in this constraint should be normalized: " + this.normal2);
      }
   }
}
