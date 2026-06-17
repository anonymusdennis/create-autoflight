package dev.simulated_team.simulated.content.blocks.redstone_magnet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.service.SimConfigService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class MagnetPair<T extends BlockEntity & SimMagnet> {
   protected final Level level;
   protected final BlockPos blockPos1;
   protected final BlockPos blockPos2;
   private final Vector3d relativePosition = new Vector3d();
   private final Vector3d relativePositionInverse = new Vector3d();
   private final Quaterniond orientation1 = new Quaterniond();
   private final Quaterniond orientation2 = new Quaterniond();
   private final Vector3d moment1 = new Vector3d();
   private final Vector3d moment2 = new Vector3d();
   private final Vector3d totalForce1 = new Vector3d();
   private final Vector3d totalTorque1 = new Vector3d();
   private final Vector3d totalForce2 = new Vector3d();
   private final Vector3d totalTorque2 = new Vector3d();
   private final Vector3d symmetricTorque = new Vector3d();
   private final Vector3d vel1 = new Vector3d();
   private final Vector3d vel2 = new Vector3d();
   private final Vector3d pos1 = new Vector3d();
   private final Vector3d pos2 = new Vector3d();
   private final Vector3d tempVel1 = new Vector3d();
   private final Vector3d tempVel2 = new Vector3d();
   public boolean alive = true;

   public MagnetPair(Level level, BlockPos pos1, BlockPos pos2) {
      this.level = level;
      this.blockPos1 = pos1;
      this.blockPos2 = pos2;
   }

   public static Vector3d getRelativePosition(SimMagnet magnet1, SimMagnet magnet2, Vector3d dest) {
      Vec3 plotPos1 = magnet1.getMagnetPosition();
      Vec3 plotPos2 = magnet2.getMagnetPosition();
      return getRelativePosition(magnet1, magnet2, plotPos1, plotPos2, dest);
   }

   public static Vector3d getRelativePosition(SimMagnet magnet1, SimMagnet magnet2, Vec3 plotPos1, Vec3 plotPos2, Vector3d dest) {
      SubLevel shell1 = magnet1.getLatestSubLevel();
      SubLevel shell2 = magnet2.getLatestSubLevel();
      Vec3 pos1 = plotPos1;
      Vec3 pos2 = plotPos2;
      if (shell1 != null) {
         pos1 = shell1.logicalPose().transformPosition(plotPos1);
      }

      if (shell2 != null) {
         pos2 = shell2.logicalPose().transformPosition(plotPos2);
      }

      return dest.set(pos1.x - pos2.x, pos1.y - pos2.y, pos1.z - pos2.z);
   }

   public void tick() {
      this.alive = false;
   }

   public void physicsTick(double timeStep) {
      BlockEntity blockEntity1 = this.level.getBlockEntity(this.blockPos1);
      BlockEntity blockEntity2 = this.level.getBlockEntity(this.blockPos2);
      if (blockEntity1 instanceof SimMagnet magnet1 && blockEntity2 instanceof SimMagnet magnet2 && magnet1.magnetActive() && magnet2.magnetActive()) {
         this.applyForces(timeStep, (T)magnet1, (T)magnet2);
      }
   }

   protected boolean canConnect(Vector3d relativePosition, Vector3d moment1, Vector3d moment2) {
      return relativePosition.lengthSquared() < 64.0;
   }

   protected double forceDistanceScale(double distance, T magnet1, T magnet2) {
      if (distance < 1.0) {
         return 1.0;
      } else {
         distance *= distance;
         distance *= distance;
         return 1.0 / distance;
      }
   }

   protected double torqueDistanceScale(double distance, T magnet1, T magnet2) {
      if (distance < 1.0) {
         return 1.0;
      } else {
         distance *= distance;
         return 1.0 / distance;
      }
   }

   public void applyForces(double timeStep, T magnet1, T magnet2) {
      ServerSubLevel subLevel1 = (ServerSubLevel)magnet1.getLatestSubLevel();
      ServerSubLevel subLevel2 = (ServerSubLevel)magnet2.getLatestSubLevel();
      if (subLevel1 != null || subLevel2 != null) {
         if (subLevel1 != null) {
            this.orientation1.set(subLevel1.logicalPose().orientation());
         } else {
            this.orientation1.identity();
         }

         if (subLevel2 != null) {
            this.orientation2.set(subLevel2.logicalPose().orientation());
         } else {
            this.orientation2.identity();
         }

         getRelativePosition(magnet1, magnet2, this.relativePosition);
         magnet1.setMagneticMoment(this.moment1);
         magnet2.setMagneticMoment(this.moment2);
         this.orientation1.transform(this.moment1);
         this.orientation2.transform(this.moment2);
         if (this.canConnect(this.relativePosition, this.moment1, this.moment2)) {
            double distance = this.relativePosition.length();
            Vector3dc magnet1Pos = JOMLConversion.toJOML(magnet1.getMagnetPosition());
            Vector3dc magnet2Pos = JOMLConversion.toJOML(magnet2.getMagnetPosition());
            MagnetPair.PairData data = new MagnetPair.PairData(
               this.relativePosition,
               this.moment1,
               this.moment2,
               magnet1,
               magnet2,
               subLevel1,
               subLevel2,
               magnet1Pos,
               magnet2Pos,
               distance,
               this.forceDistanceScale(distance, magnet1, magnet2),
               this.torqueDistanceScale(distance, magnet1, magnet2)
            );
            MagnetPair.PairData dataInverse = new MagnetPair.PairData(
               this.relativePosition.negate(this.relativePositionInverse),
               this.moment2,
               this.moment1,
               magnet2,
               magnet1,
               subLevel2,
               subLevel1,
               magnet2Pos,
               magnet1Pos,
               distance,
               data.forceScale,
               data.torqueScale
            );
            this.getSymmetricTorque(data, this.symmetricTorque);
            this.getForce(data, this.totalForce1);
            this.getTorque(data, this.totalTorque1).add(this.symmetricTorque);
            this.getTorque(dataInverse, this.totalTorque2).sub(this.symmetricTorque);
            this.totalTorque1.sub(this.totalTorque2, this.symmetricTorque);
            double linearInverseMass = this.getLinearInverseMass(data, this.totalForce1);
            this.totalForce2.set(this.totalForce1).mul(linearInverseMass);
            double forceScale = this.totalForce2.lengthSquared() > this.getAccelerationLimit() * this.getAccelerationLimit()
               ? this.getAccelerationLimit() / this.totalForce2.length()
               : 1.0;
            double angularInverseMass = this.getAngularInverseMass(data, this.symmetricTorque);
            this.totalForce2.set(this.symmetricTorque).mul(angularInverseMass);
            double torqueScale = this.totalForce2.lengthSquared() > this.getAngularAccelerationLimit() * this.getAngularAccelerationLimit()
               ? this.getAngularAccelerationLimit() / this.totalForce2.length()
               : 1.0;
            double mixedForceScale = Math.min(forceScale, torqueScale);
            this.totalForce1.mul(mixedForceScale);
            this.totalTorque1.mul(mixedForceScale);
            this.totalTorque2.mul(mixedForceScale);
            this.symmetricTorque.mul(mixedForceScale);
            if (this.getDampingRatio() > 0.0) {
               double s = -this.moment1.dot(this.moment2);
               this.dampForce(this.totalForce1, data, s * forceScale);
               this.dampTorque(this.symmetricTorque, this.symmetricTorque, data, s * torqueScale);
               this.totalTorque1.add(this.symmetricTorque);
               this.totalTorque2.sub(this.symmetricTorque);
            }

            SubLevelPhysicsSystem physicsSystem = SubLevelContainer.getContainer((ServerLevel)this.level).physicsSystem();

            assert physicsSystem != null;

            this.totalForce2.set(this.totalForce1).negate();
            if (subLevel1 != null) {
               this.orientation1.transformInverse(this.totalForce1);
               this.orientation1.transformInverse(this.totalTorque1);
               QueuedForceGroup forceTotal1 = subLevel1.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.MAGNETIC_FORCE.get());
               forceTotal1.applyAndRecordPointForce(JOMLConversion.toJOML(magnet1.getMagnetPosition()), this.totalForce1.mul(timeStep));
               forceTotal1.getForceTotal().applyLinearAndAngularImpulse(new Vector3d(), this.totalTorque1.mul(timeStep));
            }

            if (subLevel2 != null) {
               this.orientation2.transformInverse(this.totalForce2);
               this.orientation2.transformInverse(this.totalTorque2);
               QueuedForceGroup forceTotal2 = subLevel2.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.MAGNETIC_FORCE.get());
               forceTotal2.applyAndRecordPointForce(JOMLConversion.toJOML(magnet2.getMagnetPosition()), this.totalForce2.mul(timeStep));
               forceTotal2.getForceTotal().applyLinearAndAngularImpulse(new Vector3d(), this.totalTorque2.mul(timeStep));
            }
         }
      }
   }

   private void dampForce(Vector3d force, MagnetPair.PairData data, double forceScale) {
      forceScale *= data.forceScale;
      Sable.HELPER.getVelocity(this.level, data.magnet1Pos, this.vel1);
      Sable.HELPER.getVelocity(this.level, data.magnet2Pos, this.vel2);
      Vector3dc relativeVel = this.vel2.sub(this.vel1);
      double inverseMass = this.getLinearInverseMass(data, relativeVel);
      double m = 1.0 / inverseMass;
      if (m * forceScale > 0.0 && inverseMass > 0.0) {
         force.fma(2.0 * this.getDampingRatio() * Math.sqrt(m * forceScale), relativeVel);
      }
   }

   private void dampTorque(Vector3d torque, Vector3d referenceDirection, MagnetPair.PairData data, double forceScale) {
      forceScale *= data.torqueScale;
      double inverseMass = this.getAngularInverseMass(data, referenceDirection);
      if (data.body1 != null) {
         RigidBodyHandle.of(data.body1).getAngularVelocity(this.vel1);
      } else {
         this.vel1.zero();
      }

      if (data.body2 != null) {
         RigidBodyHandle.of(data.body2).getAngularVelocity(this.vel2);
      } else {
         this.vel2.zero();
      }

      Vector3d relativeVel = this.vel2.sub(this.vel1);
      referenceDirection.mul(referenceDirection.dot(relativeVel), relativeVel).div(referenceDirection.lengthSquared() + 1.0E-12);
      double m = 1.0 / inverseMass;
      if (m * forceScale > 0.0 && inverseMass > 0.0) {
         torque.set(relativeVel).mul(2.0 * this.getDampingRatio() * Math.sqrt(m * forceScale));
      }
   }

   private double getLinearInverseMass(MagnetPair.PairData data, Vector3dc referenceDirection) {
      double inverseMass = 0.0;
      if (data.body1 != null) {
         this.tempVel1.set(referenceDirection);
         data.body1.logicalPose().orientation().transformInverse(this.tempVel1);
         inverseMass += data.body1.getMassTracker().getInverseNormalMass(data.magnet1Pos, this.tempVel1);
      }

      if (data.body2 != null) {
         this.tempVel2.set(referenceDirection);
         data.body2.logicalPose().orientation().transformInverse(this.tempVel2);
         inverseMass += data.body2.getMassTracker().getInverseNormalMass(data.magnet2Pos, this.tempVel2);
      }

      return inverseMass;
   }

   private double getAngularInverseMass(MagnetPair.PairData data, Vector3dc referenceDirection) {
      double inverseMass = 0.0;
      inverseMass += data.body1 != null ? this.getRotationalInverseMass(data.body1, referenceDirection) : 0.0;
      return inverseMass + (data.body2 != null ? this.getRotationalInverseMass(data.body2, referenceDirection) : 0.0);
   }

   private double getRotationalInverseMass(ServerSubLevel body, Vector3dc direction) {
      Vector3d v = new Vector3d();
      return this.applyGlobalInertiaTensorInverse(body, v.set(direction)).dot(direction) / (direction.lengthSquared() + 1.0E-12);
   }

   private Vector3d applyGlobalInertiaTensorInverse(ServerSubLevel body, Vector3d v) {
      body.logicalPose().orientation().transformInverse(v);
      body.getMassTracker().getInverseInertiaTensor().transform(v);
      body.logicalPose().orientation().transform(v);
      return v;
   }

   public double getDampingRatio() {
      return 0.0;
   }

   public double getAccelerationLimit() {
      return (Double)SimConfigService.INSTANCE.server().physics.redstoneMagnetLinearAccelerationClamping.get();
   }

   public double getAngularAccelerationLimit() {
      return (Double)SimConfigService.INSTANCE.server().physics.redstoneMagnetAngularAccelerationClamping.get();
   }

   protected Vector3d getForce(MagnetPair.PairData data, Vector3d f) {
      f.set(data.moment2).mul(data.moment1.dot(data.relativePosition));
      f.fma(data.moment2.dot(data.relativePosition), data.moment1);
      f.fma(data.moment1.dot(data.moment2), data.relativePosition);
      f.fma(
         -5.0 * data.moment1.dot(data.relativePosition) * data.moment2.dot(data.relativePosition) / data.relativePosition.lengthSquared(),
         data.relativePosition
      );
      f.mul(data.forceScale / 2.0);
      return f;
   }

   protected Vector3d getTorque(MagnetPair.PairData data, Vector3d t) {
      t.set(data.relativePosition).mul(3.0 * data.moment2.dot(data.relativePosition) / (data.distance * data.distance));
      t.sub(data.moment2);
      data.moment1.cross(t, t);
      t.mul(data.torqueScale / 6.0);
      return t;
   }

   protected Vector3d getSymmetricTorque(MagnetPair.PairData data, Vector3d t) {
      return t.zero();
   }

   protected static record PairData(
      Vector3dc relativePosition,
      Vector3dc moment1,
      Vector3dc moment2,
      SimMagnet magnet1,
      SimMagnet magnet2,
      ServerSubLevel body1,
      ServerSubLevel body2,
      Vector3dc magnet1Pos,
      Vector3dc magnet2Pos,
      double distance,
      double forceScale,
      double torqueScale
   ) {
   }
}
