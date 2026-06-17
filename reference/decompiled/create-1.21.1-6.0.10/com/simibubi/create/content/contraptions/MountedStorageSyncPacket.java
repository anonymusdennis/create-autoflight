package com.simibubi.create.content.contraptions;

import com.simibubi.create.AllPackets;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import java.util.HashMap;
import java.util.Map;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MountedStorageSyncPacket(int contraptionId, Map<BlockPos, MountedItemStorage> items, Map<BlockPos, MountedFluidStorage> fluids)
   implements ClientboundPacketPayload {
   public static final StreamCodec<RegistryFriendlyByteBuf, MountedStorageSyncPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      MountedStorageSyncPacket::contraptionId,
      ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedItemStorage.STREAM_CODEC),
      MountedStorageSyncPacket::items,
      ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedFluidStorage.STREAM_CODEC),
      MountedStorageSyncPacket::fluids,
      MountedStorageSyncPacket::new
   );

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.MOUNTED_STORAGE_SYNC;
   }

   public void handle(LocalPlayer player) {
      if (Minecraft.getInstance().level.getEntity(this.contraptionId) instanceof AbstractContraptionEntity contraption) {
         contraption.getContraption().getStorage().handleSync(this, contraption);
      }
   }
}
