package dev.simulated_team.simulated.ponder.elements.rope;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class PonderRopePose {
   public final Vector3d start = new Vector3d();
   public final Vector3d end = new Vector3d();
   public double length;
   public double sog;
   public double floorHeight;

   public PonderRopePose() {
   }

   public PonderRopePose(Vector3d start, Vector3d end, double length, double sog, double floorHeight) {
      this.start.set(start);
      this.end.set(end);
      this.length = length;
      this.sog = sog;
      this.floorHeight = floorHeight;
   }

   public void set(PonderRopePose pose) {
      this.start.set(pose.start);
      this.end.set(pose.end);
      this.length = pose.length;
      this.sog = pose.sog;
      this.floorHeight = pose.floorHeight;
   }

   public void lerp(PonderRopePose other, double t) {
      this.start.lerp(other.start, t);
      this.end.lerp(other.end, t);
      this.length = Mth.lerp(t, this.length, other.length);
      this.sog = Mth.lerp(t, this.sog, other.sog);
   }

   public void lerp(PonderRopePose a, PonderRopePose b, PonderRopePose dest, double t) {
      a.start.lerp(b.start, t, dest.start);
      a.end.lerp(b.end, t, dest.end);
      dest.length = Mth.lerp(t, a.length, b.length);
      dest.sog = Mth.lerp(t, a.sog, b.sog);
   }

   public void lerp(Vec3 from, Vec3 to, double length, double sog, double t) {
      this.start.lerp(JOMLConversion.toJOML(from), t);
      this.end.lerp(JOMLConversion.toJOML(to), t);
      this.length = Mth.lerp(t, this.length, length);
      this.sog = Mth.lerp(t, this.sog, sog);
   }
}
