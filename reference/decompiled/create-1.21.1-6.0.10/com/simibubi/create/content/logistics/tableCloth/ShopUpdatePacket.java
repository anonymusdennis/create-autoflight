package com.simibubi.create.content.logistics.tableCloth;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.BlockEntityDataPacket;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

public class ShopUpdatePacket extends BlockEntityDataPacket<TableClothBlockEntity> {
   public static final StreamCodec<ByteBuf, ShopUpdatePacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(ShopUpdatePacket::new, i -> i.pos);

   public ShopUpdatePacket(BlockPos pos) {
      super(pos);
   }

   protected void handlePacket(TableClothBlockEntity be) {
      if (be.hasLevel()) {
         be.invalidateItemsForRender();
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.SHOP_UPDATE;
   }
}
