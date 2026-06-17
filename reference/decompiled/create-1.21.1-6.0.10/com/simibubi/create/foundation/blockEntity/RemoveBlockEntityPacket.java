package com.simibubi.create.foundation.blockEntity;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

public class RemoveBlockEntityPacket extends BlockEntityDataPacket<SyncedBlockEntity> {
   public static final StreamCodec<ByteBuf, RemoveBlockEntityPacket> STREAM_CODEC = BlockPos.STREAM_CODEC
      .map(RemoveBlockEntityPacket::new, packet -> packet.pos);

   public RemoveBlockEntityPacket(BlockPos pos) {
      super(pos);
   }

   @Override
   protected void handlePacket(SyncedBlockEntity be) {
      if (!be.hasLevel()) {
         be.setRemoved();
      } else {
         be.getLevel().removeBlockEntity(this.pos);
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.REMOVE_TE;
   }
}
