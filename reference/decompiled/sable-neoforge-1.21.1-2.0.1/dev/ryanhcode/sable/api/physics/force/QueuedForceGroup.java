package dev.ryanhcode.sable.api.physics.force;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import org.joml.Vector3dc;

public class QueuedForceGroup {
   private final List<QueuedForceGroup.PointForce> appliedForces = new ObjectArrayList();
   private final ForceTotal forceTotal = new ForceTotal();
   private final ServerSubLevel subLevel;

   public QueuedForceGroup(ServerSubLevel serverSubLevel) {
      this.subLevel = serverSubLevel;
   }

   public ForceTotal getForceTotal() {
      return this.forceTotal;
   }

   public void applyAndRecordPointForce(Vector3dc point, Vector3dc force) {
      this.forceTotal.applyImpulseAtPoint(this.subLevel.getMassTracker(), point, force);
      this.recordPointForce(point, force);
   }

   public void recordPointForce(Vector3dc point, Vector3dc force) {
      if (this.subLevel.isTrackingIndividualQueuedForces()) {
         if (force.lengthSquared() > 1.0E-6) {
            this.appliedForces.add(new QueuedForceGroup.PointForce(point, force));
         }
      }
   }

   public List<QueuedForceGroup.PointForce> getRecordedPointForces() {
      return this.appliedForces;
   }

   public void reset() {
      this.forceTotal.reset();
      this.appliedForces.clear();
   }

   public static record PointForce(Vector3dc point, Vector3dc force) {
   }
}
