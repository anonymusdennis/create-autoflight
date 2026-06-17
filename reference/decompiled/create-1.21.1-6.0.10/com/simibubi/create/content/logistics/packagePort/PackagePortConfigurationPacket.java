package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public class PackagePortConfigurationPacket extends BlockEntityConfigurationPacket<PackagePortBlockEntity> {
   public static final StreamCodec<ByteBuf, PackagePortConfigurationPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      packet -> packet.pos,
      ByteBufCodecs.STRING_UTF8,
      packet -> packet.newFilter,
      ByteBufCodecs.BOOL,
      packet -> packet.acceptPackages,
      PackagePortConfigurationPacket::new
   );
   private final String newFilter;
   private final boolean acceptPackages;

   public PackagePortConfigurationPacket(BlockPos pos, String newFilter, boolean acceptPackages) {
      super(pos);
      this.newFilter = newFilter;
      this.acceptPackages = acceptPackages;
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.PACKAGE_PORT_CONFIGURATION;
   }

   protected void applySettings(ServerPlayer player, PackagePortBlockEntity be) {
      if (!be.addressFilter.equals(this.newFilter) || be.acceptsPackages != this.acceptPackages) {
         be.addressFilter = this.newFilter;
         be.acceptsPackages = this.acceptPackages;
         be.filterChanged();
         be.notifyUpdate();
      }
   }
}
