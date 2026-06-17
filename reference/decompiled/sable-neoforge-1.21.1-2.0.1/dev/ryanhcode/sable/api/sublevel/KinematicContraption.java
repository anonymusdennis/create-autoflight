package dev.ryanhcode.sable.api.sublevel;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.floating_block.FloatingClusterContainer;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.joml.Quaterniond;
import org.joml.Vector3dc;

public interface KinematicContraption {
   void sable$getLocalBounds(BoundingBox3i var1);

   BlockGetter sable$blockGetter();

   MassTracker sable$getMassTracker();

   Vector3dc sable$getPosition(double var1);

   Quaterniond sable$getOrientation(double var1);

   Map<BlockPos, BlockSubLevelLiftProvider.LiftProviderContext> sable$liftProviders();

   FloatingClusterContainer sable$getFloatingClusterContainer();

   boolean sable$shouldCollide();

   boolean sable$isValid();

   default Vector3dc sable$getPosition() {
      return this.sable$getPosition(1.0);
   }

   default Quaterniond sable$getOrientation() {
      return this.sable$getOrientation(1.0);
   }

   default Pose3d sable$getLocalPose(Pose3d dest, double partialTick) {
      dest.rotationPoint().set(this.sable$getMassTracker().getCenterOfMass());
      dest.position().set(this.sable$getPosition(partialTick));
      dest.orientation().set(this.sable$getOrientation(partialTick));
      dest.scale().set(JOMLConversion.ONE);
      return dest;
   }
}
