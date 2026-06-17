package dev.simulated_team.simulated.content.blocks.rope.strand.server;

import dev.ryanhcode.sable.api.sublevel.SubLevelTrackingPlugin;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class ServerRopeTrackingSystem implements SubLevelTrackingPlugin {
   private final ServerLevel level;

   public ServerRopeTrackingSystem(ServerLevel level) {
      this.level = level;
   }

   private ServerLevelRopeManager getRopeManager() {
      return ServerLevelRopeManager.getOrCreate(this.level);
   }

   public Iterable<UUID> neededPlayers() {
      ServerLevelRopeManager ropeManager = this.getRopeManager();
      Collection<ServerRopeStrand> strands = ropeManager.getAllStrands();
      if (strands.isEmpty()) {
         return List.of();
      } else {
         Set<UUID> players = new ObjectOpenHashSet();

         for (ServerRopeStrand strand : strands) {
            if (strand.isActive()) {
               strand.updatePose();
               if (strand.needsSync() || !strand.networkingStopped) {
                  RopeAttachment attachment = strand.getAttachment(RopeAttachmentPoint.START);
                  BlockPos block = attachment.blockAttachment();
                  RopeStrandHolderBehavior holder = (RopeStrandHolderBehavior)RopeStrandHolderBehavior.get(
                     this.level.getBlockEntity(block), RopeStrandHolderBehavior.TYPE
                  );
                  if (holder != null) {
                     for (ServerPlayer player : holder.getStrandTrackingPlayers()) {
                        players.add(player.getUUID());
                     }
                  }
               }
            }
         }

         return players;
      }
   }

   public void sendTrackingData(int interpolationTick) {
      ServerLevelRopeManager ropeManager = this.getRopeManager();

      for (ServerRopeStrand strand : ropeManager.getAllStrands()) {
         if (strand.isActive()) {
            if (strand.needsSync()) {
               strand.networkingStopped = false;
               RopeAttachment attachment = strand.getAttachment(RopeAttachmentPoint.START);
               BlockPos block = attachment.blockAttachment();
               RopeStrandHolderBehavior holder = (RopeStrandHolderBehavior)RopeStrandHolderBehavior.get(
                  this.level.getBlockEntity(block), RopeStrandHolderBehavior.TYPE
               );
               if (holder != null) {
                  holder.getStrandPacketSink().sendPacket(new CustomPacketPayload[]{holder.makeUpdatePacket()});
                  strand.justSynced();
               }
            } else if (!strand.networkingStopped) {
               strand.networkingStopped = true;
               RopeAttachment attachment = strand.getAttachment(RopeAttachmentPoint.START);
               BlockPos block = attachment.blockAttachment();
               RopeStrandHolderBehavior holder = (RopeStrandHolderBehavior)RopeStrandHolderBehavior.get(
                  this.level.getBlockEntity(block), RopeStrandHolderBehavior.TYPE
               );
               if (holder != null) {
                  holder.getStrandPacketSink().sendPacket(new CustomPacketPayload[]{holder.makeStopPacket()});
               }
            }
         }
      }
   }
}
