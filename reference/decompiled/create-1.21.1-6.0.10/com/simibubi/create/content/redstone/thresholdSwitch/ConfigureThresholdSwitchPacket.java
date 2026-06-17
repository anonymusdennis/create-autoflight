package com.simibubi.create.content.redstone.thresholdSwitch;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public class ConfigureThresholdSwitchPacket extends BlockEntityConfigurationPacket<ThresholdSwitchBlockEntity> {
   public static final StreamCodec<ByteBuf, ConfigureThresholdSwitchPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      packet -> packet.pos,
      ByteBufCodecs.INT,
      packet -> packet.offBelow,
      ByteBufCodecs.INT,
      packet -> packet.onAbove,
      ByteBufCodecs.BOOL,
      packet -> packet.invert,
      ByteBufCodecs.BOOL,
      packet -> packet.inStacks,
      ConfigureThresholdSwitchPacket::new
   );
   private final int offBelow;
   private final int onAbove;
   private final boolean invert;
   private final boolean inStacks;

   public ConfigureThresholdSwitchPacket(BlockPos pos, int offBelow, int onAbove, boolean invert, boolean inStacks) {
      super(pos);
      this.offBelow = offBelow;
      this.onAbove = onAbove;
      this.invert = invert;
      this.inStacks = inStacks;
   }

   protected void applySettings(ServerPlayer player, ThresholdSwitchBlockEntity be) {
      be.offWhenBelow = this.offBelow;
      be.onWhenAbove = this.onAbove;
      be.setInverted(this.invert);
      be.inStacks = this.inStacks;
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONFIGURE_STOCKSWITCH;
   }
}
