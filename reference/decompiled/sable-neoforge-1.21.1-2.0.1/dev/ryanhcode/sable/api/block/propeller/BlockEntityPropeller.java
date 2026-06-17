package dev.ryanhcode.sable.api.block.propeller;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;

public interface BlockEntityPropeller {
   Direction getBlockDirection();

   double getAirflow();

   double getThrust();

   boolean isActive();

   default double getScaledThrust() {
      return -this.getThrust() * this.getAirflowScaling() * this.getCurrentAirPressure();
   }

   default double getCurrentAirPressure() {
      Level level = this.getLevel();
      return DimensionPhysicsData.getAirPressure(level, Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.toJOML(this.getBlockPos().getCenter())));
   }

   default double getAirflowScaling() {
      double airflow = this.getAirflow();
      if (Math.abs(airflow) <= 0.001) {
         return 1.0;
      } else {
         Level level = this.getLevel();
         Vector3d pos = JOMLConversion.toJOML(this.getBlockPos().getCenter());
         SubLevel subLevel = Sable.HELPER.getContaining(level, this.getBlockPos());
         if (subLevel == null) {
            return 1.0;
         } else {
            Vector3d velocity = Sable.HELPER.getVelocity(level, subLevel, pos, new Vector3d());
            Vector3d thrustDirection = subLevel.logicalPose().transformNormal(JOMLConversion.atLowerCornerOf(this.getBlockDirection().getNormal()));
            return Math.clamp((airflow + velocity.dot(thrustDirection.x, thrustDirection.y, thrustDirection.z)) / airflow, 0.0, 1.0);
         }
      }
   }

   Level getLevel();

   BlockPos getBlockPos();
}
