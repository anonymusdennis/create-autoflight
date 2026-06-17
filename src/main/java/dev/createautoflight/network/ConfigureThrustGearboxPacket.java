package dev.createautoflight.network;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import dev.createautoflight.content.thrust.ThrustVectoringGearboxBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.createmod.catnip.net.base.BasePacketPayload;

public class ConfigureThrustGearboxPacket extends BlockEntityConfigurationPacket<ThrustVectoringGearboxBlockEntity> {
    public static final StreamCodec<ByteBuf, ConfigureThrustGearboxPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, p -> p.pos,
            ByteBufCodecs.INT, p -> p.axis,
            ConfigureThrustGearboxPacket::new
    );

    private final int axis;

    public ConfigureThrustGearboxPacket(BlockPos pos, int axis) {
        super(pos);
        this.axis = axis;
    }

    public ConfigureThrustGearboxPacket(BlockPos pos, Direction axis) {
        this(pos, axis.get3DDataValue());
    }

    @Override
    protected void applySettings(ServerPlayer player, ThrustVectoringGearboxBlockEntity be) {
        be.setThrustAxis(Direction.from3DDataValue(Math.clamp(axis, 0, 5)));
    }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return ModPackets.CONFIGURE_THRUST_GEARBOX;
    }
}
