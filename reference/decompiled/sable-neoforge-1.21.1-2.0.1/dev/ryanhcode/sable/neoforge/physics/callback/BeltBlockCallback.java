package dev.ryanhcode.sable.neoforge.physics.callback;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class BeltBlockCallback implements BlockSubLevelCollisionCallback {
   public static BeltBlockCallback INSTANCE = new BeltBlockCallback();

   private BeltBlockCallback() {
   }

   @Override
   public BlockSubLevelCollisionCallback.CollisionResult sable$onCollision(
      BlockPos pos, @Nullable BlockPos otherHitBlockPos, Vector3d pos1, double impactVelocity
   ) {
      SubLevelPhysicsSystem system = SubLevelPhysicsSystem.getCurrentlySteppingSystem();
      ServerLevel level = system.getLevel();
      if (level.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
         BlockState state = belt.getBlockState();
         Direction facing = (Direction)state.getValue(BeltBlock.HORIZONTAL_FACING);
         BeltSlope slope = (BeltSlope)state.getValue(BeltBlock.SLOPE);
         if (slope == BeltSlope.SIDEWAYS) {
            return BlockSubLevelCollisionCallback.CollisionResult.NONE;
         } else {
            Vec3i normal = Direction.get(AxisDirection.POSITIVE, facing.getAxis()).getNormal();
            float speed = belt.getBeltMovementSpeed() * 20.0F;
            if (facing.getAxis() == Axis.X) {
               speed *= -1.0F;
            }

            Vector3d velocity = new Vector3d(
               (double)((float)normal.getX() * speed), (double)((float)normal.getY() * speed), (double)((float)normal.getZ() * speed)
            );
            if (slope == BeltSlope.HORIZONTAL && pos1.y - (double)belt.getBlockPos().getY() < 0.5) {
               velocity.negate();
            }

            return new BlockSubLevelCollisionCallback.CollisionResult(velocity, false);
         }
      } else {
         return BlockSubLevelCollisionCallback.CollisionResult.NONE;
      }
   }
}
