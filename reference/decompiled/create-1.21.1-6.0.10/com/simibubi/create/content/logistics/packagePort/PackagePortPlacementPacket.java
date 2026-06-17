package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.AllPackets;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record PackagePortPlacementPacket(PackagePortTarget target, BlockPos pos) implements ServerboundPacketPayload {
   public static final StreamCodec<RegistryFriendlyByteBuf, PackagePortPlacementPacket> STREAM_CODEC = StreamCodec.composite(
      PackagePortTarget.STREAM_CODEC,
      PackagePortPlacementPacket::target,
      BlockPos.STREAM_CODEC,
      PackagePortPlacementPacket::pos,
      PackagePortPlacementPacket::new
   );

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.PLACE_PACKAGE_PORT;
   }

   public void handle(ServerPlayer player) {
      if (player != null) {
         Level world = player.level();
         if (world != null && world.isLoaded(this.pos)) {
            if (world.getBlockEntity(this.pos) instanceof PackagePortBlockEntity ppbe) {
               if (this.target.canSupport(ppbe)) {
                  Vec3 targetLocation = this.target.getExactTargetLocation(ppbe, world, this.pos);
                  if (targetLocation != Vec3.ZERO
                     && targetLocation.closerThan(Vec3.atBottomCenterOf(this.pos), (double)((Integer)AllConfigs.server().logistics.packagePortRange.get() + 2))
                     )
                   {
                     this.target.setup(ppbe, world, this.pos);
                     ppbe.target = this.target;
                     ppbe.notifyUpdate();
                     ppbe.use(player);
                  }
               }
            }
         }
      }
   }

   public static record ClientBoundRequest(BlockPos pos) implements ClientboundPacketPayload {
      public static final StreamCodec<ByteBuf, PackagePortPlacementPacket.ClientBoundRequest> STREAM_CODEC = BlockPos.STREAM_CODEC
         .map(PackagePortPlacementPacket.ClientBoundRequest::new, PackagePortPlacementPacket.ClientBoundRequest::pos);

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.S_PLACE_PACKAGE_PORT;
      }

      public void handle(LocalPlayer player) {
         PackagePortTargetSelectionHandler.flushSettings(this.pos);
      }
   }
}
