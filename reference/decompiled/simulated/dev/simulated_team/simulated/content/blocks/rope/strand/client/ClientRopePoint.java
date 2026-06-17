package dev.simulated_team.simulated.content.blocks.rope.strand.client;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.joml.Vector3d;

public record ClientRopePoint(Vector3d position, Vector3d previousPosition, ObjectList<ClientRopePoint.Snapshot> snapshots) {
   public Vector3d renderPos(float partialTicks, Vector3d dest) {
      return dest.set(this.previousPosition).lerp(this.position, (double)partialTicks);
   }

   public static record Snapshot(double interpolationTick, Vector3d position) {
   }
}
