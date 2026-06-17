package dev.ryanhcode.sable.api.physics.constraint;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3dc;

public sealed interface PhysicsConstraintConfiguration<T extends PhysicsConstraintHandle>
   permits FixedConstraintConfiguration,
   FreeConstraintConfiguration,
   GenericConstraintConfiguration,
   RotaryConstraintConfiguration {
   static void validateAnchors(ServerSubLevelContainer container, PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB, Vector3dc pos1, Vector3dc pos2) {
      if (bodyA instanceof ServerSubLevel subLevel) {
         if (!subLevel.getPlot().contains(pos1)) {
            throw new IllegalArgumentException(
               "pos1 does not fall within the plot of the first sub-level in block-coordinates! Double check your coordinate spaces."
            );
         }
      } else if (container.inBounds(pos1)) {
         throw new IllegalArgumentException(
            "the first body of this constraint is not a sub-level, but the first position is in the plotgrid! Double check your coordinate spaces."
         );
      }

      if (bodyB instanceof ServerSubLevel subLevelx) {
         if (!subLevelx.getPlot().contains(pos2)) {
            throw new IllegalArgumentException(
               "pos2 does not fall within the plot of the second sub-level in block-coordinates! Double check your coordinate spaces."
            );
         }
      } else if (container.inBounds(pos2)) {
         throw new IllegalArgumentException(
            "the second body of this constraint is not a sub-level, but the second position is in the plotgrid! Double check your coordinate spaces."
         );
      }
   }

   default void validate(ServerSubLevelContainer container, PhysicsPipelineBody bodyA, PhysicsPipelineBody bodyB) {
   }
}
