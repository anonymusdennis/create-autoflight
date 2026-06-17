package dev.simulated_team.simulated.network.packets;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.altitude_sensor.AltitudeSensorBlockEntity;
import dev.simulated_team.simulated.network.packets.helpers.SimBlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ConfigureAltitudeSensorPacket extends SimBlockEntityConfigurationPacket<AltitudeSensorBlockEntity> {
   public static final Type<ConfigureAltitudeSensorPacket> TYPE = new Type(Simulated.path("configure_altitude_sensor"));
   public static final StreamCodec<ByteBuf, ConfigureAltitudeSensorPacket> CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      SimBlockEntityConfigurationPacket::getPos,
      ByteBufCodecs.FLOAT,
      ConfigureAltitudeSensorPacket::highSignal,
      ByteBufCodecs.FLOAT,
      ConfigureAltitudeSensorPacket::lowSignal,
      ConfigureAltitudeSensorPacket::new
   );
   private final float highSignal;
   private final float lowSignal;

   public ConfigureAltitudeSensorPacket(BlockPos pos, float highSignal, float lowSignal) {
      super(pos);
      this.highSignal = Mth.clamp(highSignal, 0.0F, 1.0F);
      this.lowSignal = Mth.clamp(lowSignal, 0.0F, 1.0F);
   }

   private float lowSignal() {
      return this.lowSignal;
   }

   private float highSignal() {
      return this.highSignal;
   }

   @NotNull
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   protected void applySettings(ServerPlayer serverPlayer, AltitudeSensorBlockEntity be) {
      if (be instanceof AltitudeSensorBlockEntity) {
         be.highSignal = this.highSignal;
         be.lowSignal = this.lowSignal;
         be.notifyUpdate();
      }
   }
}
