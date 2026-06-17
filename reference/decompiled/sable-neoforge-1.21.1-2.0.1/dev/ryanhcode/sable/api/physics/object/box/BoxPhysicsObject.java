package dev.ryanhcode.sable.api.physics.object.box;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.physics.object.ArbitraryPhysicsObject;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class BoxPhysicsObject implements ArbitraryPhysicsObject, PhysicsPipelineBody {
   protected BoxHandle handle;
   private final Pose3d pose = new Pose3d();
   private final Vector3d halfExtents = new Vector3d();
   private final double mass;
   private boolean active = false;

   public BoxPhysicsObject(Pose3dc pose, Vector3dc halfExtents, double mass) {
      this.pose.set(pose);
      this.halfExtents.set(halfExtents);
      this.mass = mass;
   }

   @Override
   public void getBoundingBox(BoundingBox3d dest) {
      double max = this.halfExtents.get(this.halfExtents.maxComponent());
      Vector3d center = this.pose.position();
      dest.set(center.x, center.y, center.z, center.x, center.y, center.z);
      dest.expand(max * 1.7321);
   }

   public void updatePose() {
      this.handle.readPose(this.pose);
   }

   @Override
   public void onUnloaded(SubLevelHoldingChunkMap holdingChunkMap, ChunkPos chunkPos) {
      this.remove();
   }

   @Override
   public void onRemoved() {
      this.remove();
   }

   protected void remove() {
      this.active = false;
      this.handle.remove();
      this.handle = null;
   }

   @Override
   public void onAddition(SubLevelPhysicsSystem physicsSystem) {
      this.active = true;
      this.handle = physicsSystem.getPipeline().addBox(this);
   }

   @Override
   public void wakeUp() {
      this.handle.wakeUp();
   }

   public Pose3dc getPose() {
      return this.pose;
   }

   public Vector3dc getHalfExtents() {
      return this.halfExtents;
   }

   public double getMass() {
      return this.mass;
   }

   public boolean isActive() {
      return this.active;
   }

   @Override
   public int getRuntimeId() {
      return this.handle == null ? -1 : this.handle.getRuntimeId();
   }

   @Override
   public MassData getMassTracker() {
      return new BoxPhysicsObject.BoxMassData();
   }

   @Override
   public boolean isRemoved() {
      return !this.active;
   }

   private class BoxMassData implements MassData {
      private final Matrix3dc inertia = new Matrix3d().scale(BoxPhysicsObject.this.mass / 6.0);
      private final Matrix3dc inverseInertia = this.inertia.invert(new Matrix3d());

      @Override
      public double getMass() {
         return BoxPhysicsObject.this.mass;
      }

      @Override
      public double getInverseMass() {
         return 1.0 / BoxPhysicsObject.this.mass;
      }

      @Override
      public Matrix3dc getInertiaTensor() {
         return this.inertia;
      }

      @Override
      public Matrix3dc getInverseInertiaTensor() {
         return this.inverseInertia;
      }

      @Nullable
      @Override
      public Vector3dc getCenterOfMass() {
         return JOMLConversion.ZERO;
      }
   }
}
