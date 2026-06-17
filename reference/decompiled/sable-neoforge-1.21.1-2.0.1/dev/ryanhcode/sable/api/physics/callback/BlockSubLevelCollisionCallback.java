package dev.ryanhcode.sable.api.physics.callback;

import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface BlockSubLevelCollisionCallback {
   @Internal
   default double[] onCollision(
      int x, int y, int z, int otherX, int otherY, int otherZ, double x1, double y1, double z1, double impactVelocity, boolean hasOtherBlock
   ) {
      BlockSubLevelCollisionCallback.CollisionResult result = this.sable$onCollision(
         new BlockPos(x, y, z), hasOtherBlock ? new BlockPos(otherX, otherY, otherZ) : null, new Vector3d(x1, y1, z1), impactVelocity
      );
      Vector3dc motion = result.tangentMotion;
      return new double[]{motion.x(), motion.y(), motion.z(), result.removeCollision ? 1.0 : 0.0};
   }

   BlockSubLevelCollisionCallback.CollisionResult sable$onCollision(BlockPos var1, @Nullable BlockPos var2, Vector3d var3, double var4);

   public static record CollisionResult(Vector3dc tangentMotion, boolean removeCollision) {
      public static final BlockSubLevelCollisionCallback.CollisionResult NONE = new BlockSubLevelCollisionCallback.CollisionResult(JOMLConversion.ZERO, false);
   }
}
