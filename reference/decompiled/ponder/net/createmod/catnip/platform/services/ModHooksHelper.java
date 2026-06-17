package net.createmod.catnip.platform.services;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public interface ModHooksHelper {
   boolean playerPlaceSingleBlock(Player var1, Level var2, BlockPos var3, BlockState var4);

   default ItemStack getCloneItemFromBlockstate(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return state.getBlock().getCloneItemStack(level, pos, state);
   }

   boolean isPlayerFake(ServerPlayer var1);
}
