package dev.ryanhcode.sable.api.physics.force;

import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class ForceTotal {
   private final Vector3d temp = new Vector3d();
   private final Vector3d lastLocalForce = new Vector3d();
   private final Vector3d lastLocalTorque = new Vector3d();
   private final Vector3d localForce = new Vector3d();
   private final Vector3d localTorque = new Vector3d();

   @Internal
   public void applyForces(RigidBodyHandle handle) {
      boolean forceChanged = this.localForce.distanceSquared(this.lastLocalForce) > 1.0E-5;
      boolean torqueChanged = this.localTorque.distanceSquared(this.lastLocalTorque) > 1.0E-5;
      boolean wakeUp = forceChanged || torqueChanged;
      handle.applyLinearAndAngularImpulse(this.localForce, this.localTorque, wakeUp);
      this.lastLocalForce.set(this.localForce);
      this.lastLocalTorque.set(this.localTorque);
      this.localForce.set(0.0, 0.0, 0.0);
      this.localTorque.set(0.0, 0.0, 0.0);
   }

   public void reset() {
      this.localForce.set(0.0, 0.0, 0.0);
      this.localTorque.set(0.0, 0.0, 0.0);
   }

   public void applyForceTotal(ForceTotal other) {
      this.localForce.add(other.localForce);
      this.localTorque.add(other.localTorque);
   }

   public void applyLinearAndAngularImpulse(Vector3dc impulse, Vector3dc torque) {
      this.localForce.add(impulse);
      this.localTorque.add(torque);
   }

   public void applyLinearImpulse(Vector3dc impulse) {
      this.applyLinearAndAngularImpulse(impulse, JOMLConversion.ZERO);
   }

   public void applyAngularImpulse(Vector3dc impulse) {
      this.applyLinearAndAngularImpulse(JOMLConversion.ZERO, impulse);
   }

   public void applyTorqueImpulse(Vector3dc torque) {
      this.applyAngularImpulse(torque);
   }

   public void applyImpulseAtPoint(MassData massTracker, Vector3dc position, Vector3dc force) {
      this.localForce.add(force);
      position.sub(massTracker.getCenterOfMass(), this.temp);
      this.localTorque.add(this.temp.cross(force));
   }

   public void applyImpulseAtPoint(ServerSubLevel massTracker, Vector3dc position, Vector3dc force) {
      this.applyImpulseAtPoint(massTracker.getMassTracker(), position, force);
   }

   public Vector3d getLocalForce() {
      return this.localForce;
   }

   public Vector3d getLocalTorque() {
      return this.localTorque;
   }

   public void applyImpulseAtPoint(MassTracker massTracker, Vec3 position, Vec3 force) {
      this.applyImpulseAtPoint(massTracker, JOMLConversion.toJOML(position), JOMLConversion.toJOML(force));
   }
}
