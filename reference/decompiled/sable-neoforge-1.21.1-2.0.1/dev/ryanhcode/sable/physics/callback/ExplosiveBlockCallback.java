package dev.ryanhcode.sable.physics.callback;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public class ExplosiveBlockCallback extends FragileBlockCallback {
   public static final ExplosiveBlockCallback INSTANCE = new ExplosiveBlockCallback();

   @Override
   public boolean shouldTriggerFor(BlockState state) {
      return state.getBlock() instanceof TntBlock;
   }

   @Override
   public double getTriggerVelocity() {
      return 5.0;
   }

   @Override
   public BlockSubLevelCollisionCallback.CollisionResult onHit(ServerLevel level, BlockPos pos, BlockState state, Vector3d hitPos) {
      PrimedTnt primedTnt = new PrimedTnt(level, (double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, null);
      primedTnt.setFuse(4);
      level.addFreshEntity(primedTnt);
      level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
      return new BlockSubLevelCollisionCallback.CollisionResult(JOMLConversion.ZERO, true);
   }
}
