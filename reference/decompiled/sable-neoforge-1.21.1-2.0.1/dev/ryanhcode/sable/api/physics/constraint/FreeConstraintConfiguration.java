package dev.ryanhcode.sable.api.physics.constraint;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

public record FreeConstraintConfiguration(Vector3dc pos1, Vector3dc pos2, Quaterniondc orientation)
   implements PhysicsConstraintConfiguration<FreeConstraintHandle> {
   @Override
   public void validate(ServerSubLevelContainer container, PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB) {
      PhysicsConstraintConfiguration.validateAnchors(container, bodyA, bodyB, this.pos1, this.pos2);
   }
}
