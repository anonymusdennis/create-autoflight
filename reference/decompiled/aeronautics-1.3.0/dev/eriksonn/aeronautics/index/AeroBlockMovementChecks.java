package dev.eriksonn.aeronautics.index;

import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import dev.eriksonn.aeronautics.content.blocks.hot_air.envelope.Envelope;
import dev.simulated_team.simulated.index.SimBlockMovementChecks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AeroBlockMovementChecks {
   private static CheckResult isBlockAttachedTowards(BlockState state, Level world, BlockPos pos, Direction direction) {
      return isBlockAttachedTowards(state, world, pos, BlockPos.ZERO.relative(direction));
   }

   private static CheckResult isBlockAttachedTowards(BlockState state, Level world, BlockPos pos, BlockPos direction) {
      return state.getBlock() instanceof Envelope && world.getBlockState(pos.offset(direction)).getBlock() instanceof Envelope
         ? CheckResult.SUCCESS
         : CheckResult.PASS;
   }

   @Internal
   public static void init() {
      BlockMovementChecks.registerAttachedCheck(AeroBlockMovementChecks::isBlockAttachedTowards);
      SimBlockMovementChecks.registerAttachedCheck(AeroBlockMovementChecks::isBlockAttachedTowards);
   }
}
