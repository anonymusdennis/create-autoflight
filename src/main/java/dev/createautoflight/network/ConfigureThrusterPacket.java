package dev.createautoflight.network;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import dev.createautoflight.content.thruster.ThrusterBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.createmod.catnip.net.base.BasePacketPayload;

public class ConfigureThrusterPacket extends BlockEntityConfigurationPacket<ThrusterBlockEntity> {
    private static final int FLAG_SMOKE = 1;
    private static final int FLAG_INVERT = 2;

    public static final StreamCodec<ByteBuf, ConfigureThrusterPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, p -> p.pos,
            ByteBufCodecs.BOOL, p -> p.enabled,
            ByteBufCodecs.INT, p -> p.mode.ordinal(),
            ByteBufCodecs.INT, p -> p.strengthPercent,
            ByteBufCodecs.INT, p -> p.maxThrust,
            ByteBufCodecs.INT, p -> packOptions(p.smokeParticles, p.invertDirection),
            (pos, enabled, modeOrd, strengthPercent, maxThrust, options) ->
                    new ConfigureThrusterPacket(
                            pos,
                            enabled,
                            ThrusterBlockEntity.ThrusterMode.values()[modeOrd],
                            strengthPercent,
                            maxThrust,
                            (options & FLAG_SMOKE) != 0,
                            (options & FLAG_INVERT) != 0
                    )
    );

    private final boolean enabled;
    private final ThrusterBlockEntity.ThrusterMode mode;
    private final int strengthPercent;
    private final int maxThrust;
    private final boolean smokeParticles;
    private final boolean invertDirection;

    public ConfigureThrusterPacket(
            BlockPos pos,
            boolean enabled,
            ThrusterBlockEntity.ThrusterMode mode,
            int strengthPercent,
            int maxThrust,
            boolean smokeParticles,
            boolean invertDirection
    ) {
        super(pos);
        this.enabled = enabled;
        this.mode = mode;
        this.strengthPercent = strengthPercent;
        this.maxThrust = maxThrust;
        this.smokeParticles = smokeParticles;
        this.invertDirection = invertDirection;
    }

    private static int packOptions(boolean smokeParticles, boolean invertDirection) {
        int flags = 0;
        if (smokeParticles) {
            flags |= FLAG_SMOKE;
        }
        if (invertDirection) {
            flags |= FLAG_INVERT;
        }
        return flags;
    }

    @Override
    protected void applySettings(ServerPlayer player, ThrusterBlockEntity be) {
        be.applyConfiguration(enabled, mode, strengthPercent, maxThrust, smokeParticles, invertDirection);
    }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return ModPackets.CONFIGURE_THRUSTER;
    }
}
