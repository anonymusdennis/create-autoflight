package dev.ryanhcode.sable.physics.callback;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity.BeeReleaseStatus;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class BeehiveBlockCallback extends FragileBlockCallback {
   public static final BeehiveBlockCallback INSTANCE = new BeehiveBlockCallback();

   @Override
   public boolean shouldTriggerFor(BlockState state) {
      return state.getBlock() instanceof BeehiveBlock;
   }

   @Override
   public double getTriggerVelocity() {
      return 9.0;
   }

   @Override
   public BlockSubLevelCollisionCallback.CollisionResult onHit(ServerLevel level, BlockPos pos, BlockState state, Vector3d hitPos) {
      if (level.getBlockEntity(pos) instanceof BeehiveBlockEntity beehiveBlockEntity) {
         Vec3 center = pos.getCenter();
         Player nearbyPlayer = level.getNearestPlayer(center.x, center.y, center.z, 4.0, true);
         beehiveBlockEntity.emptyAllLivingFromHive(nearbyPlayer, state, BeeReleaseStatus.EMERGENCY);
      }

      return new BlockSubLevelCollisionCallback.CollisionResult(JOMLConversion.ZERO, false);
   }
}
