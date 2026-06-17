package com.simibubi.create.content.redstone.displayLink;

import com.simibubi.create.AllPackets;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class DisplayLinkConfigurationPacket extends BlockEntityConfigurationPacket<DisplayLinkBlockEntity> {
   public static final StreamCodec<ByteBuf, DisplayLinkConfigurationPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      packet -> packet.pos,
      ByteBufCodecs.COMPOUND_TAG,
      packet -> packet.configData,
      ByteBufCodecs.VAR_INT,
      packet -> packet.targetLine,
      DisplayLinkConfigurationPacket::new
   );
   private final CompoundTag configData;
   private final int targetLine;

   public DisplayLinkConfigurationPacket(BlockPos pos, CompoundTag configData, int targetLine) {
      super(pos);
      this.configData = configData;
      this.targetLine = targetLine;
   }

   protected void applySettings(ServerPlayer player, DisplayLinkBlockEntity be) {
      be.targetLine = this.targetLine;
      if (!this.configData.contains("Id")) {
         be.notifyUpdate();
      } else {
         ResourceLocation id = ResourceLocation.tryParse(this.configData.getString("Id"));
         DisplaySource source = DisplaySource.get(id);
         if (source == null) {
            be.notifyUpdate();
         } else {
            if (be.activeSource != null && be.activeSource == source) {
               be.getSourceConfig().merge(this.configData);
            } else {
               be.activeSource = source;
               be.setSourceConfig(this.configData.copy());
            }

            be.updateGatheredData();
            be.notifyUpdate();
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONFIGURE_DATA_GATHERER;
   }
}
