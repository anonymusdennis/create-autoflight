package dev.simulated_team.simulated.neoforge.mixin.multiloader;

import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({CommonRedstoneBlock.class})
public interface CommonRedstoneBlockMixin extends CommonRedstoneBlock, IBlockExtension {
   default boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return this.commonCheckWeakPower(state, level, pos, side);
   }

   default boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
      return this.commonConnectRedstone(state, level, pos, direction);
   }
}
