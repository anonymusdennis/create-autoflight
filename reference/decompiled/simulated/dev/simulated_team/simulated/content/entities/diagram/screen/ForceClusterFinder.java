package dev.simulated_team.simulated.content.entities.diagram.screen;

import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup.PointForce;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.mutable.MutableInt;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class ForceClusterFinder {
   private static final double CLUSTER_SEPARATION_THRESHOLD = 0.4;

   public static List<ForceClusterFinder.Cluster> passThrough(List<PointForce> forces) {
      List<ForceClusterFinder.Cluster> clusters = new ArrayList<>(forces.size());

      for (PointForce force : forces) {
         clusters.add(new ForceClusterFinder.Cluster(new Vector3d(force.point()), new Vector3d(force.force()), new MutableInt(1)));
      }

      return clusters;
   }

   public static List<ForceClusterFinder.Cluster> getMergedClusters(List<PointForce> forces) {
      List<ForceClusterFinder.Cluster> clusters = new ArrayList<>();
      if (forces.isEmpty()) {
         return clusters;
      } else {
         List<ForceClusterFinder.ClusteredForce> clusteredForces = new ArrayList<>();

         for (PointForce force : forces) {
            clusteredForces.add(new ForceClusterFinder.ClusteredForce(force.point(), force.force(), new MutableInt()));
         }

         while (tryAddCluster(clusters, clusteredForces)) {
            while (!groupArrows(clusters, clusteredForces)) {
               organizeClusters(clusters, clusteredForces);
            }
         }

         organizeClusters(clusters, clusteredForces);
         finalizeClusters(clusters, clusteredForces);
         return clusters;
      }
   }

   static boolean tryAddCluster(List<ForceClusterFinder.Cluster> clusters, List<ForceClusterFinder.ClusteredForce> forces) {
      if (!clusters.isEmpty()) {
         double maxDistance = -1.0;
         ForceClusterFinder.ClusteredForce index = null;

         for (ForceClusterFinder.ClusteredForce force : forces) {
            double d = getVariance(clusters.get(force.getIndex()).force, force.force);
            if (d > maxDistance) {
               maxDistance = d;
               index = force;
            }
         }

         if (index != null && maxDistance > 0.16000000000000003) {
            ForceClusterFinder.Cluster c = new ForceClusterFinder.Cluster(new Vector3d(), new Vector3d(index.force), new MutableInt());
            clusters.add(c);
            return true;
         } else {
            return false;
         }
      } else {
         ForceClusterFinder.Cluster c = new ForceClusterFinder.Cluster(new Vector3d(), new Vector3d(), new MutableInt());

         for (ForceClusterFinder.ClusteredForce forcex : forces) {
            c.force.add(Math.abs(forcex.force.x()), Math.abs(forcex.force.y()), Math.abs(forcex.force.z()));
         }

         c.force.normalize();
         clusters.add(c);
         return true;
      }
   }

   static boolean groupArrows(List<ForceClusterFinder.Cluster> clusters, List<ForceClusterFinder.ClusteredForce> forces) {
      boolean done = true;

      for (ForceClusterFinder.ClusteredForce force : forces) {
         int previousIndex = force.getIndex();
         double minDist = 100.0;

         for (int i = 0; i < clusters.size(); i++) {
            double dist = getVariance(force.force, clusters.get(i).force);
            if (dist < minDist) {
               minDist = dist;
               force.clusterIndex.setValue(i);
            }
         }

         if (previousIndex != force.getIndex()) {
            done = false;
         }
      }

      return done;
   }

   static void organizeClusters(List<ForceClusterFinder.Cluster> clusters, List<ForceClusterFinder.ClusteredForce> forces) {
      for (ForceClusterFinder.Cluster c : clusters) {
         c.force.zero();
         c.groupSize.setValue(0);
      }

      for (ForceClusterFinder.ClusteredForce force : forces) {
         ForceClusterFinder.Cluster c = clusters.get(force.getIndex());
         c.force.add(force.force);
         c.groupSize.increment();
      }

      for (int k = clusters.size() - 1; k >= 0; k--) {
         ForceClusterFinder.Cluster c = clusters.get(k);
         if (c.groupSize.getValue() == 0) {
            clusters.remove(c);

            for (ForceClusterFinder.ClusteredForce force : forces) {
               if (force.clusterIndex.getValue() > k) {
                  force.clusterIndex.decrement();
               }
            }
         }
      }
   }

   static void finalizeClusters(List<ForceClusterFinder.Cluster> clusters, List<ForceClusterFinder.ClusteredForce> forces) {
      for (ForceClusterFinder.ClusteredForce force : forces) {
         ForceClusterFinder.Cluster c = clusters.get(force.getIndex());
         c.pos.fma(c.force.dot(force.force) / c.force.lengthSquared(), force.pos);
      }
   }

   static double getVariance(Vector3dc x, Vector3dc y) {
      double x2 = x.dot(x);
      double xy = x.dot(y);
      double y2 = y.dot(y);
      return 2.0 * (1.0 - xy / Math.sqrt(x2 * y2));
   }

   public static record Cluster(Vector3d pos, Vector3d force, MutableInt groupSize) {
   }

   static record ClusteredForce(Vector3dc pos, Vector3dc force, MutableInt clusterIndex) {
      int getIndex() {
         return this.clusterIndex.getValue();
      }
   }
}
