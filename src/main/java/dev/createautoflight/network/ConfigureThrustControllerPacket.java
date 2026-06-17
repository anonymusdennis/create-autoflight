package dev.createautoflight.network;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import dev.createautoflight.content.thrust.DynamicThrustControllerBlockEntity;
import dev.createautoflight.content.thrust.ThrustControlMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.createmod.catnip.net.base.BasePacketPayload;

public class ConfigureThrustControllerPacket extends BlockEntityConfigurationPacket<DynamicThrustControllerBlockEntity> {
    public static final StreamCodec<ByteBuf, ConfigureThrustControllerPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, p -> p.pos,
            ByteBufCodecs.INT, p -> p.mode,
            ByteBufCodecs.INT, p -> p.gainPercent,
            ByteBufCodecs.INT, p -> p.maxSlewRpm,
            ByteBufCodecs.INT, p -> packFlags(p.debugOverlay, p.holdAltitude),
            ByteBufCodecs.INT, p -> p.targetHeightY,
            (pos, mode, gainPercent, maxSlewRpm, flags, targetHeightY) -> new ConfigureThrustControllerPacket(
                    pos,
                    mode,
                    gainPercent,
                    maxSlewRpm,
                    (flags & 1) != 0,
                    (flags & 2) != 0,
                    targetHeightY
            )
    );

    private static final int FLAG_DEBUG = 1;
    private static final int FLAG_HOLD_ALT = 2;

    private static int packFlags(boolean debugOverlay, boolean holdAltitude) {
        int flags = 0;
        if (debugOverlay) {
            flags |= FLAG_DEBUG;
        }
        if (holdAltitude) {
            flags |= FLAG_HOLD_ALT;
        }
        return flags;
    }

    private final int mode;
    private final int gainPercent;
    private final int maxSlewRpm;
    private final boolean debugOverlay;
    private final boolean holdAltitude;
    private final int targetHeightY;

    public ConfigureThrustControllerPacket(
            BlockPos pos,
            int mode,
            int gainPercent,
            int maxSlewRpm,
            boolean debugOverlay,
            boolean holdAltitude,
            int targetHeightY
    ) {
        super(pos);
        this.mode = mode;
        this.gainPercent = gainPercent;
        this.maxSlewRpm = maxSlewRpm;
        this.debugOverlay = debugOverlay;
        this.holdAltitude = holdAltitude;
        this.targetHeightY = targetHeightY;
    }

    @Override
    protected void applySettings(ServerPlayer player, DynamicThrustControllerBlockEntity be) {
        be.applyConfiguration(
                ThrustControlMode.values()[Math.clamp(mode, 0, ThrustControlMode.values().length - 1)],
                gainPercent,
                maxSlewRpm,
                debugOverlay,
                holdAltitude,
                targetHeightY
        );
    }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return ModPackets.CONFIGURE_THRUST_CONTROLLER;
    }
}
