package dev.ryanhcode.sable.physics.floating_block;

import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.util.SableMathUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class FloatingBlockController {
   private static final Vector3d frictionForce = new Vector3d();
   private static final Vector3d frictionTorque = new Vector3d();
   private static final Vector3d clusterFrictionForce = new Vector3d();
   private static final Vector3d clusterFrictionTorque = new Vector3d();
   private static final Vector3d localGravity = new Vector3d();
   private static final Vector3d localLinearVelocity = new Vector3d();
   private static final Vector3d localAngularVelocity = new Vector3d();
   private final FloatingClusterContainer sublevelContainer = new FloatingClusterContainer();
   private final List<FloatingClusterContainer> containers = new ArrayList<>();
   private final ServerSubLevel subLevel;
   private final Vector3d previousCenterOfMass = new Vector3d();
   private static final Vector3d totalWeightedForce = new Vector3d();
   private static final Vector3d averageForcePos = new Vector3d();
   private static final Vector3d liftingForce = new Vector3d();
   private static final Vector3d liftingTorque = new Vector3d();
   private static final Vector3d torqueTemp = new Vector3d();
   private static final Vector3d weightedPositionTemp = new Vector3d();
   private static final Vector3d totalAcceleration = new Vector3d();
   private static final Matrix3d containerRotation = new Matrix3d();
   private static final Vector3d clusterCenter = new Vector3d();
   private static final Vector3d totalAngularVelocity = new Vector3d();
   private static final Vector3d rotatedPos = new Vector3d();
   private static final Matrix3d slowDragMatrix = new Matrix3d();
   private static final Matrix3d fastDragMatrix = new Matrix3d();
   private static final Matrix3d averagePositionMatrix = new Matrix3d();
   private static final Matrix3d averagePositionMatrixInverse = new Matrix3d();
   private static final Matrix3d shiftedPositionMatrix = new Matrix3d();
   private static final Matrix3d shiftedPositionMatrixInverse = new Matrix3d();
   private static final Matrix3d tempTorqueMatrix = new Matrix3d();
   private static final Vector3d meanVelocity = new Vector3d();
   private static final Vector3d shiftedCenter = new Vector3d();
   private static final Vector3d linearSlowDrag = new Vector3d();
   private static final Matrix3d X2 = new Matrix3d();
   private static final Matrix3d Y2 = new Matrix3d();
   private static final Matrix3d YX = new Matrix3d();
   private static final Matrix3d traceMatrix = new Matrix3d();

   public FloatingBlockController(ServerSubLevel subLevel) {
      this.subLevel = subLevel;
   }

   public void physicsTick(
      double partialPhysicsTick, double timeStep, Vector3dc linearVelocity, Vector3dc angularVelocity, Vector3d linearImpulse, Vector3d angularImpulse
   ) {
      this.containers.clear();
      this.containers.add(this.sublevelContainer);

      for (KinematicContraption contraption : this.subLevel.getPlot().getContraptions()) {
         FloatingClusterContainer container = contraption.sable$getFloatingClusterContainer();
         Vector3dc lastPosition = new Vector3d(contraption.sable$getPosition(partialPhysicsTick - 1.0));
         Quaterniondc lastOrientation = new Quaterniond(contraption.sable$getOrientation(partialPhysicsTick - 1.0));
         container.positionOffset.set(contraption.sable$getPosition(partialPhysicsTick));
         container.rotationOffset.set(contraption.sable$getOrientation(partialPhysicsTick));
         container.positionOffset.sub(lastPosition, container.velocity);
         SableMathUtils.getAngularVelocity(lastOrientation, container.rotationOffset, container.angularVelocity);
         container.velocity.mul(20.0);
         container.angularVelocity.mul(20.0);
         container.positionOffset.sub(this.subLevel.getMassTracker().getCenterOfMass());
         this.containers.add(container);
      }

      this.processBlockChanges();
      localGravity.set(DimensionPhysicsData.getGravity(this.subLevel.getLevel(), this.subLevel.logicalPose().position()));
      this.subLevel.logicalPose().orientation().transformInverse(localGravity);
      if (this.needsTicking()) {
         this.subLevel.logicalPose().orientation().transformInverse(linearVelocity, localLinearVelocity);
         this.subLevel.logicalPose().orientation().transformInverse(angularVelocity, localAngularVelocity);
         frictionForce.zero();
         frictionTorque.zero();
         QueuedForceGroup dragGroup = this.subLevel.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.DRAG.get());
         List<Vector3d> recordedFrictionForces = new ObjectArrayList();

         for (FloatingClusterContainer container : this.containers) {
            for (FloatingBlockCluster cluster : container.clusters) {
               if (cluster.getMaterial().scaleWithPressure()) {
                  cluster.getBlockData().computePressureScale(this.subLevel);
               }

               this.applyFriction(container, cluster, localGravity, localLinearVelocity, localAngularVelocity, clusterFrictionForce, clusterFrictionTorque);
               Vector3d recordedClusterFrictionForce = new Vector3d(clusterFrictionForce);
               this.recordForce(container, cluster, dragGroup, recordedClusterFrictionForce);
               recordedFrictionForces.add(recordedClusterFrictionForce);
               frictionForce.add(clusterFrictionForce);
               frictionTorque.add(clusterFrictionTorque);
            }
         }

         for (Vector3d force : recordedFrictionForces) {
            force.mul(timeStep);
         }

         if (localGravity.lengthSquared() > 0.0) {
            this.applyLift(localGravity, linearImpulse, angularImpulse, timeStep);
         }

         linearImpulse.fma(timeStep, frictionForce);
         angularImpulse.fma(timeStep, frictionTorque);
      }
   }

   public boolean needsTicking() {
      if (this.sublevelContainer.needsTicking()) {
         return true;
      } else {
         for (FloatingClusterContainer container : this.containers) {
            if (container.needsTicking()) {
               return true;
            }
         }

         return false;
      }
   }

   private void processBlockChanges() {
      this.previousCenterOfMass.sub(this.subLevel.getMassTracker().getCenterOfMass());

      for (FloatingBlockCluster cluster : this.sublevelContainer.clusters) {
         cluster.getBlockData().translateOrigin(this.previousCenterOfMass);
      }

      this.sublevelContainer.processBlockChanges(this.subLevel.getMassTracker().getCenterOfMass());
      this.previousCenterOfMass.set(this.subLevel.getMassTracker().getCenterOfMass());
   }

   private void applyLift(Vector3d localGravity, Vector3d linearImpulse, Vector3d angularImpulse, double timeStep) {
      double totalForce = 0.0;
      totalWeightedForce.set(0.0);

      for (FloatingClusterContainer container : this.containers) {
         for (FloatingBlockCluster cluster : container.clusters) {
            FloatingBlockMaterial material = cluster.getMaterial();
            if (material.liftStrength() != 0.0) {
               double clusterForce = material.liftStrength();
               if (material.scaleWithPressure()) {
                  clusterForce *= cluster.getBlockData().getPressureScale();
               }

               double weightedForce = clusterForce * cluster.getBlockData().totalScale;
               this.getTrueWeightedClusterPosition(container, cluster, weightedPositionTemp);
               if (material.preventSelfLift()) {
                  totalForce += weightedForce;
                  totalWeightedForce.fma(clusterForce, weightedPositionTemp);
               } else {
                  linearImpulse.fma(-weightedForce * timeStep, localGravity);
                  if (this.subLevel.isTrackingIndividualQueuedForces()) {
                     QueuedForceGroup levitationGroup = this.subLevel.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.LEVITATION.get());
                     this.recordForce(container, cluster, levitationGroup, new Vector3d(localGravity).mul(-weightedForce * timeStep));
                  }

                  localGravity.cross(weightedPositionTemp, torqueTemp);
                  angularImpulse.fma(clusterForce * timeStep, torqueTemp);
               }
            }
         }
      }

      if (!(totalForce <= 0.0)) {
         totalWeightedForce.div(totalForce, averageForcePos);
         liftingForce.set(localGravity).mul(-totalForce);
         averageForcePos.cross(liftingForce, liftingTorque);
         this.subLevel.getMassTracker().getInverseInertiaTensor().transform(liftingTorque, torqueTemp).cross(averageForcePos, totalAcceleration);
         totalAcceleration.fma(1.0 / this.subLevel.getMassTracker().getMass(), liftingForce);
         double scaleFactor = -localGravity.lengthSquared() / localGravity.dot(totalAcceleration);
         if (scaleFactor > 1.0) {
            scaleFactor = 1.0;
         }

         liftingForce.mul(scaleFactor);
         liftingTorque.mul(scaleFactor);
         if (this.subLevel.isTrackingIndividualQueuedForces()) {
            QueuedForceGroup levitationGroup = this.subLevel.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.LEVITATION.get());

            for (FloatingClusterContainer container : this.containers) {
               for (FloatingBlockCluster clusterx : container.clusters) {
                  FloatingBlockMaterial material = clusterx.getMaterial();
                  Vector3d force = new Vector3d(localGravity).mul(timeStep * -clusterx.getBlockData().totalScale * material.liftStrength());
                  force.mul(scaleFactor);
                  this.recordForce(container, clusterx, levitationGroup, force);
               }
            }
         }

         linearImpulse.fma(timeStep, liftingForce);
         angularImpulse.fma(timeStep, liftingTorque);
      }
   }

   private void recordForce(FloatingClusterContainer container, FloatingBlockCluster cluster, QueuedForceGroup forceGroup, Vector3d force) {
      forceGroup.recordPointForce(
         this.getTrueWeightedClusterPosition(container, cluster, new Vector3d())
            .div(cluster.getBlockData().totalScale)
            .add(this.subLevel.getMassTracker().getCenterOfMass()),
         force
      );
   }

   private Vector3d getTrueWeightedClusterPosition(FloatingClusterContainer container, FloatingBlockCluster cluster, Vector3d pos) {
      container.rotationOffset.transform(cluster.getBlockData().weightedPosition, pos);
      return pos.fma(cluster.getBlockData().totalScale, container.positionOffset);
   }

   private void applyFriction(
      FloatingClusterContainer container,
      FloatingBlockCluster cluster,
      Vector3dc localGravity,
      Vector3dc linearVelocity,
      Vector3dc angularVelocity,
      Vector3d frictionForce,
      Vector3d frictionTorque
   ) {
      double frictionScale = 1.0;
      if (cluster.getMaterial().scaleWithGravity()) {
         frictionScale = localGravity.length();
      }

      if (cluster.getMaterial().scaleWithPressure()) {
         frictionScale *= cluster.getBlockData().getPressureScale();
      }

      double speedScale = 3.0 / (cluster.getMaterial().transitionSpeed() * cluster.getMaterial().transitionSpeed());
      if (cluster.getMaterial().transitionSpeed() == 0.0) {
         speedScale = 0.0;
      }

      totalAngularVelocity.set(angularVelocity).add(container.angularVelocity);
      this.getTrueWeightedClusterPosition(container, cluster, clusterCenter).div(cluster.getBlockData().totalScale);
      cluster.getBlockData().outerProduct.scale(1.0 / cluster.getBlockData().totalScale, averagePositionMatrix);
      container.rotationOffset.get(containerRotation);
      averagePositionMatrix.mulLocal(containerRotation);
      averagePositionMatrix.mul(containerRotation.transpose());
      averagePositionMatrix.invert(averagePositionMatrixInverse);
      shiftedPositionMatrixInverse.set(averagePositionMatrixInverse);
      SableMathUtils.fmaInertiaTensor(totalAngularVelocity, speedScale, shiftedPositionMatrixInverse);
      shiftedPositionMatrixInverse.invert(shiftedPositionMatrix);
      angularVelocity.cross(clusterCenter, meanVelocity);
      container.rotationOffset.transform(cluster.getBlockData().weightedPosition, rotatedPos).div(cluster.getBlockData().totalScale);
      Vector3d extraContainerVelocity = container.angularVelocity.cross(rotatedPos, rotatedPos);
      meanVelocity.add(linearVelocity).add(container.velocity).add(extraContainerVelocity);
      totalAngularVelocity.cross(meanVelocity, shiftedCenter).mul(speedScale);
      shiftedPositionMatrix.transform(shiftedCenter);
      double slowDragScale = Math.sqrt(shiftedPositionMatrix.determinant() / averagePositionMatrix.determinant());
      slowDragScale *= Math.exp(
         -0.5 * (speedScale * meanVelocity.dot(meanVelocity) - SableMathUtils.multiplyInnerProduct(shiftedCenter, shiftedPositionMatrixInverse, shiftedCenter))
      );
      if (cluster.getMaterial().transitionSpeed() == 0.0) {
         slowDragScale = 0.0;
      }

      this.getGravityMatrix(localGravity, cluster.getMaterial().slowVerticalFriction(), cluster.getMaterial().slowHorizontalFriction(), slowDragMatrix)
         .scale(cluster.getBlockData().totalScale * frictionScale * slowDragScale);
      this.getGravityMatrix(localGravity, cluster.getMaterial().fastVerticalFriction(), cluster.getMaterial().fastHorizontalFriction(), fastDragMatrix)
         .scale(cluster.getBlockData().totalScale * frictionScale);
      slowDragMatrix.transform(totalAngularVelocity.cross(shiftedCenter, linearSlowDrag).add(meanVelocity));
      fastDragMatrix.transform(meanVelocity, frictionForce).add(linearSlowDrag);
      clusterCenter.cross(frictionForce, frictionTorque);
      Vector3d torqueTemp = shiftedCenter.cross(linearSlowDrag, linearSlowDrag);
      frictionTorque.add(torqueTemp);
      tempTorqueMatrix.zero();
      this.matrixThingy(averagePositionMatrix, fastDragMatrix, tempTorqueMatrix);
      this.matrixThingy(shiftedPositionMatrix, slowDragMatrix, tempTorqueMatrix);
      tempTorqueMatrix.transform(totalAngularVelocity, torqueTemp);
      frictionTorque.add(torqueTemp);
   }

   private void matrixThingy(Matrix3dc X, Matrix3dc Y, Matrix3d out) {
      Y.mul(X, YX);
      double traceX = X.m00() + X.m11() + X.m22();
      double traceY = Y.m00() + Y.m11() + Y.m22();
      double traceYX = YX.m00() + YX.m11() + YX.m22();
      traceMatrix.identity().scale(traceX).sub(X, X2);
      traceMatrix.identity().scale(traceY).sub(Y, Y2);
      traceMatrix.identity().scale(traceYX).sub(YX, YX);
      X2.mul(Y2);
      out.add(X2).sub(YX);
   }

   private Matrix3d getGravityMatrix(Vector3dc g, double verticalDrag, double horizontalDrag, Matrix3d target) {
      if (g.lengthSquared() > 1.0E-5) {
         SableMathUtils.setOuterProduct(g, g, (horizontalDrag - verticalDrag) / g.dot(g), target);
      } else {
         target.identity();
      }

      target.m00 -= horizontalDrag;
      target.m11 -= horizontalDrag;
      target.m22 -= horizontalDrag;
      return target;
   }

   private double getClampingFactor(Vector3dc currentVelocity, Vector3dc expectedVelocityChange) {
      double k = -currentVelocity.dot(expectedVelocityChange);
      double v = currentVelocity.lengthSquared();
      if (k < 0.0) {
         return 0.0;
      } else if (10.0 * k < v) {
         return 1.0;
      } else {
         return v < 1.0E-10 ? v / (k + 1.0E-10) : v * (1.0 - Math.exp(-k / v)) / k;
      }
   }

   private double getKineticClampingFactor(
      Vector3dc currentLinearVelocity, Vector3dc currentAngularVelocity, Vector3d frictionForce, Vector3d frictionTorque, double timestep
   ) {
      double numerator = currentLinearVelocity.dot(frictionForce) + currentAngularVelocity.dot(frictionTorque);
      double denominator = frictionForce.dot(frictionForce) * this.subLevel.getMassTracker().getInverseMass()
         + SableMathUtils.multiplyInnerProduct(frictionTorque, this.subLevel.getMassTracker().getInverseInertiaTensor(), frictionTorque);
      denominator *= timestep;
      if (denominator < 1.0E-10) {
         return 1.0;
      } else {
         double t = -numerator / denominator;
         return Math.max(Math.min(t, 1.0), 0.0);
      }
   }

   public void addFloatingBlock(BlockState state, Vector3d pos) {
      this.sublevelContainer.addFloatingBlock(state, pos);
   }

   public void removeFloatingBlock(BlockState state, Vector3d pos) {
      this.sublevelContainer.removeFloatingBlock(state, pos);
   }

   public void queueAddFloatingBlock(BlockState state, BlockPos pos) {
      this.sublevelContainer.queueAddFloatingBlock(state, pos);
   }

   public void queueRemoveFloatingBlock(BlockState state, BlockPos pos) {
      this.sublevelContainer.queueRemoveFloatingBlock(state, pos);
   }
}
