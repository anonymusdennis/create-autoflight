package dev.simulated_team.simulated.ponder.instructions;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3d;
import org.joml.Vector3d;

public class OBBOutlineInstruction extends TickingInstruction {
   final AABB bb;
   final Vec3 rotation;
   final boolean bigLines;
   final PonderPalette color;
   final String slot;
   final Vec3 pivotPoint;
   final Matrix3d m;
   final Vector3d[] lines;

   public OBBOutlineInstruction(AABB bb, Vec3 rotation, boolean bigLines, PonderPalette color, String slot, int ticks) {
      super(false, ticks);
      this.bb = bb;
      this.rotation = rotation;
      this.bigLines = bigLines;
      this.color = color;
      this.pivotPoint = bb.getCenter();
      this.m = getRotationMatrix(rotation.x, rotation.y, rotation.z);
      this.lines = getAABBLines(bb);
      this.slot = slot;

      for (Vector3d line : this.lines) {
         this.m.transform(line.sub(this.pivotPoint.x, this.pivotPoint.y, this.pivotPoint.z)).add(this.pivotPoint.x, this.pivotPoint.y, this.pivotPoint.z);
      }
   }

   public void tick(PonderScene scene) {
      super.tick(scene);

      for (int i = 0; i < this.lines.length; i += 2) {
         scene.getOutliner()
            .showLine(this.slot + i, JOMLConversion.toMojang(this.lines[i]), JOMLConversion.toMojang(this.lines[i + 1]))
            .lineWidth(this.bigLines ? 0.125F : 0.0625F)
            .colored(this.color.getColor());
      }
   }

   @NotNull
   private static Vector3d[] getAABBLines(AABB bb) {
      double minX = bb.minX;
      double maxX = bb.maxX;
      double minY = bb.minY;
      double maxY = bb.maxY;
      double minZ = bb.minZ;
      double maxZ = bb.maxZ;
      return new Vector3d[]{
         new Vector3d(minX, maxY, minZ),
         new Vector3d(maxX, maxY, minZ),
         new Vector3d(minX, maxY, minZ),
         new Vector3d(minX, maxY, maxZ),
         new Vector3d(maxX, maxY, maxZ),
         new Vector3d(maxX, maxY, minZ),
         new Vector3d(maxX, maxY, maxZ),
         new Vector3d(minX, maxY, maxZ),
         new Vector3d(minX, minY, minZ),
         new Vector3d(maxX, minY, minZ),
         new Vector3d(minX, minY, minZ),
         new Vector3d(minX, minY, maxZ),
         new Vector3d(maxX, minY, maxZ),
         new Vector3d(maxX, minY, minZ),
         new Vector3d(maxX, minY, maxZ),
         new Vector3d(minX, minY, maxZ),
         new Vector3d(minX, minY, minZ),
         new Vector3d(minX, maxY, minZ),
         new Vector3d(maxX, minY, minZ),
         new Vector3d(maxX, maxY, minZ),
         new Vector3d(minX, minY, maxZ),
         new Vector3d(minX, maxY, maxZ),
         new Vector3d(maxX, minY, maxZ),
         new Vector3d(maxX, maxY, maxZ)
      };
   }

   @NotNull
   private static Matrix3d getRotationMatrix(double x, double y, double z) {
      double sinA = Math.sin(Math.toRadians(x));
      double cosA = Math.cos(Math.toRadians(x));
      double sinB = Math.sin(Math.toRadians(y));
      double cosB = Math.cos(Math.toRadians(y));
      double sinY = Math.sin(Math.toRadians(z));
      double cosY = Math.cos(Math.toRadians(z));
      return new Matrix3d(
         cosB * cosY,
         sinA * sinB * cosY - cosA * sinY,
         cosA * sinB * cosY + sinA * sinY,
         cosB * sinY,
         sinA * sinB * sinY + cosA * cosY,
         cosA * sinB * sinY - sinA * cosY,
         -sinB,
         sinA * cosB,
         cosA * cosB
      );
   }
}
