package dev.simulated_team.simulated.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface QuietUse {
   @Nullable
   InteractionResult quietUse(Player var1, InteractionHand var2, BlockPos var3, BlockState var4);
}
