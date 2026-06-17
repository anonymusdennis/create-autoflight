package dev.simulated_team.simulated.content.entities.diagram;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;

record DiagramRecordingTicket(ServerSubLevel subLevel, List<ServerPlayer> players) {
   public boolean isValid() {
      for (ServerPlayer player : this.players) {
         if (player.isRemoved() || player.level() != this.subLevel.getLevel()) {
            return false;
         }
      }

      return this.subLevel != null && !this.subLevel.isRemoved();
   }
}
