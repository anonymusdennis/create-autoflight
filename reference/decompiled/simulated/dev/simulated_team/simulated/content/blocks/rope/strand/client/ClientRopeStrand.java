package dev.simulated_team.simulated.content.blocks.rope.strand.client;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.UUID;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class ClientRopeStrand {
   private final ObjectArrayList<ClientRopePoint> points = new ObjectArrayList();
   public Vec3 startAttachment = null;
   public Vec3 endAttachment = null;
   private boolean stopped;
   private final UUID uuid;

   public ClientRopeStrand(UUID uuid) {
      this.uuid = uuid;
   }

   public ObjectArrayList<ClientRopePoint> getPoints() {
      return this.points;
   }

   protected void tickInterpolation(double gameTick) {
      ObjectListIterator var3 = this.points.iterator();

      while (var3.hasNext()) {
         ClientRopePoint point = (ClientRopePoint)var3.next();
         ObjectList<ClientRopePoint.Snapshot> buffer = point.snapshots();
         point.previousPosition().set(point.position());

         while (!buffer.isEmpty() && ((ClientRopePoint.Snapshot)buffer.getFirst()).interpolationTick() < gameTick - 6.0) {
            buffer.removeFirst();
         }

         if (!buffer.isEmpty()) {
            int beforeIndex = -1;
            ClientRopePoint.Snapshot before = null;
            ClientRopePoint.Snapshot after = null;

            for (int i = 0; i < buffer.size(); i++) {
               ClientRopePoint.Snapshot snapshot = (ClientRopePoint.Snapshot)buffer.get(i);
               if (gameTick == snapshot.interpolationTick()) {
                  point.position().set(snapshot.position());
               } else if (snapshot.interpolationTick() < gameTick) {
                  beforeIndex = i;
                  before = snapshot;
               } else if (snapshot.interpolationTick() > gameTick) {
                  after = snapshot;
                  break;
               }
            }

            if (before != null && after != null) {
               double factor = (gameTick - before.interpolationTick()) / (after.interpolationTick() - before.interpolationTick());
               before.position().lerp(after.position(), factor, point.position());
            } else if (before != null) {
               point.position().set(before.position());
               int beforeBeforeIndex = beforeIndex - 1;
               if (beforeBeforeIndex >= 0 && !this.stopped) {
                  ClientRopePoint.Snapshot beforeBefore = (ClientRopePoint.Snapshot)buffer.get(beforeBeforeIndex);
                  double deadReckoningTicks = Mth.clamp(gameTick - before.interpolationTick(), 0.0, 1.0);
                  double fraction = deadReckoningTicks / (before.interpolationTick() - beforeBefore.interpolationTick());
                  point.position().set(beforeBefore.position()).lerp(before.position(), 1.0 + fraction);
               }
            } else if (after != null) {
               point.position().set(after.position());
            }
         }
      }
   }

   public void setStopped(boolean stopped) {
      this.stopped = stopped;
   }

   public AABB getBounds() {
      if (this.points.isEmpty()) {
         return null;
      } else {
         Vector3d point0 = ((ClientRopePoint)this.points.getFirst()).position();
         AABB bounds = new AABB(point0.x, point0.y, point0.z, point0.x, point0.y, point0.z);
         ObjectListIterator var3 = this.points.iterator();

         while (var3.hasNext()) {
            ClientRopePoint point = (ClientRopePoint)var3.next();
            Vector3d pos = point.position();
            bounds = bounds.minmax(new AABB(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z));
         }

         return bounds;
      }
   }

   public UUID getUuid() {
      return this.uuid;
   }
}
