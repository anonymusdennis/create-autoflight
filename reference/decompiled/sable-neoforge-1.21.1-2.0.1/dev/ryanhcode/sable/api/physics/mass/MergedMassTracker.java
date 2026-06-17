package dev.ryanhcode.sable.api.physics.mass;

import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.SableMathUtils;
import java.util.Collection;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class MergedMassTracker implements MassData {
   private final MassTracker selfTracker;
   private final ServerSubLevel subLevel;
   private double mass;
   private final Matrix3d inertiaTensor = new Matrix3d().zero();
   private double inverseMass;
   private final Matrix3d inverseInertiaTensor = new Matrix3d().zero();
   @Nullable
   private Vector3d centerOfMass;
   private double lastMass;
   @Nullable
   private Vector3d lastCenterOfMass;
   @Nullable
   private Matrix3d lastInertiaTensor;

   public MergedMassTracker(@NotNull ServerSubLevel subLevel, MassTracker selfTracker) {
      this.subLevel = subLevel;
      this.selfTracker = selfTracker;
   }

   public void update(float partialPhysicsTick) {
      if (this.selfTracker.getCenterOfMass() != null) {
         Collection<KinematicContraption> contraptions = this.subLevel.getPlot().getContraptions();
         this.mass = this.selfTracker.getMass();
         this.centerOfMass = this.selfTracker.getCenterOfMass().mul(this.getMass(), new Vector3d());

         for (KinematicContraption contraption : contraptions) {
            MassTracker contraptionMassData = contraption.sable$getMassTracker();
            this.mass = this.getMass() + contraptionMassData.getMass();
            this.centerOfMass.fma(contraptionMassData.getMass(), contraption.sable$getPosition((double)partialPhysicsTick));
         }

         this.centerOfMass.mul(1.0 / this.getMass());
         this.inertiaTensor.set(this.selfTracker.getInertiaTensor());
         Vector3d localShift = this.centerOfMass.sub(this.selfTracker.getCenterOfMass(), new Vector3d());
         if (localShift.lengthSquared() > 0.0) {
            SableMathUtils.fmaInertiaTensor(localShift, this.selfTracker.getMass(), this.inertiaTensor);
         }

         for (KinematicContraption contraption : contraptions) {
            MassTracker contraptionMassData = contraption.sable$getMassTracker();
            Vector3d localPos = contraption.sable$getPosition((double)partialPhysicsTick).sub(this.centerOfMass, new Vector3d());
            SableMathUtils.fmaInertiaTensor(localPos, contraptionMassData.getMass(), this.inertiaTensor);
            Quaterniond contraptionOrientation = contraption.sable$getOrientation((double)partialPhysicsTick);
            Matrix3d localInertiaTensor = new Matrix3d()
               .rotateLocal(contraptionOrientation.conjugate(new Quaterniond()))
               .mulLocal(contraptionMassData.getInertiaTensor())
               .rotateLocal(contraptionOrientation);
            this.inertiaTensor.add(localInertiaTensor);
         }

         this.inverseMass = 1.0 / this.mass;
         this.inertiaTensor.invert(this.inverseInertiaTensor);
         this.uploadData();
         this.setPreviousValues();
      }
   }

   private void uploadData() {
      if (this.centerOfMass != null
         && (
            this.mass != this.lastMass
               || !Objects.equals(this.lastCenterOfMass, this.centerOfMass)
               || !Objects.equals(this.lastInertiaTensor, this.inertiaTensor)
         )) {
         if (this.lastCenterOfMass == null || this.lastInertiaTensor == null) {
            this.lastCenterOfMass = new Vector3d(this.centerOfMass);
            this.lastInertiaTensor = new Matrix3d(this.inertiaTensor);
         }

         ServerLevel level = this.subLevel.getLevel();
         ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
         SubLevelPhysicsSystem physicsSystem = container.physicsSystem();
         Vector3d movement = this.centerOfMass.sub(this.lastCenterOfMass, new Vector3d());
         physicsSystem.updatePose(this.subLevel);
         Pose3d pose = this.subLevel.logicalPose();
         physicsSystem.getPipeline().teleport(this.subLevel, pose.position().add(pose.orientation().transform(movement)), pose.orientation());
         pose.rotationPoint().set(this.centerOfMass);
         physicsSystem.getPipeline().onStatsChanged(this.subLevel);
      }
   }

   private void setPreviousValues() {
      if (this.centerOfMass == null) {
         this.lastCenterOfMass = null;
         this.lastInertiaTensor = null;
      } else {
         if (this.lastCenterOfMass == null) {
            this.lastCenterOfMass = new Vector3d();
            this.lastInertiaTensor = new Matrix3d().zero();
         }

         this.lastCenterOfMass.set(this.centerOfMass);
         this.lastInertiaTensor.set(this.inertiaTensor);
      }

      this.lastMass = this.mass;
   }

   @Override
   public double getInverseMass() {
      return this.inverseMass;
   }

   @Override
   public Matrix3dc getInverseInertiaTensor() {
      return this.inverseInertiaTensor;
   }

   @Override
   public Matrix3dc getInertiaTensor() {
      return this.inertiaTensor;
   }

   @Override
   public double getMass() {
      return this.mass;
   }

   @Override
   public Vector3dc getCenterOfMass() {
      return this.centerOfMass;
   }

   public MassTracker getSelfMassTracker() {
      return this.selfTracker;
   }
}
