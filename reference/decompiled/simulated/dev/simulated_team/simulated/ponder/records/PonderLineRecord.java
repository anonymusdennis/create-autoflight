package dev.simulated_team.simulated.ponder.records;

import net.minecraft.world.phys.Vec3;

public record PonderLineRecord(Vec3 startPos, Vec3 endPos) {
   public static PonderLineRecord withOffset(Vec3 startPos, Vec3 endPos) {
      return new PonderLineRecord(startPos.add(87.0, 0.0, 0.0), endPos.add(87.0, 0.0, 0.0));
   }

   public static PonderLineRecord withOffset(double startPosX, double startPosY, double endPosX, double endPosY) {
      return new PonderLineRecord(new Vec3(startPosX + 87.0, startPosY, 0.0), new Vec3(endPosX + 87.0, endPosY, 0.0));
   }
}
