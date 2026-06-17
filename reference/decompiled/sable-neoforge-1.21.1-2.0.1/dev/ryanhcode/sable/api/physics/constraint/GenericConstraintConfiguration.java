package dev.ryanhcode.sable.api.physics.constraint;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import java.util.EnumSet;
import java.util.Set;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

public record GenericConstraintConfiguration(
   Vector3dc pos1, Vector3dc pos2, Quaterniondc orientation1, Quaterniondc orientation2, Set<ConstraintJointAxis> lockedAxes
) implements PhysicsConstraintConfiguration<GenericConstraintHandle> {
   public GenericConstraintConfiguration(Vector3dc pos1, Vector3dc pos2, Quaterniondc orientation1, Quaterniondc orientation2) {
      this(pos1, pos2, orientation1, orientation2, EnumSet.noneOf(ConstraintJointAxis.class));
   }

   @Override
   public void validate(ServerSubLevelContainer container, PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB) {
      PhysicsConstraintConfiguration.validateAnchors(container, bodyA, bodyB, this.pos1, this.pos2);
   }
}
