package dev.simulated_team.simulated.util;

import dev.simulated_team.simulated.network.packets.BlockEntityObservedPacket;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public interface Observable {
   default void onObserved(Player player) {
   }

   default void sendObserved(BlockPos pos) {
      VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new BlockEntityObservedPacket(pos)});
   }
}
