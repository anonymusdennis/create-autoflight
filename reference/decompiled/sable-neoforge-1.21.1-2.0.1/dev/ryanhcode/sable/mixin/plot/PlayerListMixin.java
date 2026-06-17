package dev.ryanhcode.sable.mixin.plot;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({PlayerList.class})
public class PlayerListMixin {
   @Shadow
   @Final
   private List<ServerPlayer> players;

   @Overwrite
   public void broadcast(@Nullable Player player, double x, double y, double z, double maxDistance, ResourceKey<Level> resourceKey, Packet<?> packet) {
      ActiveSableCompanion helper = Sable.HELPER;

      for (ServerPlayer value : this.players) {
         Level level = value.level();
         if (value != player && level.dimension() == resourceKey) {
            double dist = helper.distanceSquaredWithSubLevels(level, value.position(), x, y, z);
            if (dist < maxDistance * maxDistance) {
               value.connection.send(packet);
            }
         }
      }
   }
}
