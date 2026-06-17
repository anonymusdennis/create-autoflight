package dev.createautoflight.network;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import dev.createautoflight.content.navigation.NavigationBlockEntity;
import dev.createautoflight.content.navigation.NavigationSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.createmod.catnip.net.base.BasePacketPayload;

public class ConfigureNavigationPacket extends BlockEntityConfigurationPacket<NavigationBlockEntity> {
    private static final int FLAG_INVERT_ANGLE = 32;
    private static final int FLAG_INVERT_THRUST = 64;

    public static final StreamCodec<ByteBuf, ConfigureNavigationPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, p -> p.pos,
            ByteBufCodecs.INT, p -> packSettings(
                    p.activated, p.debugOverlay, p.ignoreTerrain, p.idleBraking, p.helicopterMode,
                    p.invertAngle, p.invertThrust, p.helicopterMaxPitchDeg, p.navMaxThrust
            ),
            ByteBufCodecs.INT, p -> p.avoidanceOffDistance,
            ByteBufCodecs.INT, p -> p.arrivalRadius,
            ByteBufCodecs.INT, p -> p.cruiseSpeedPercent,
            ByteBufCodecs.INT, p -> p.slowSpeedPercent,
            (pos, settings, avoidanceOffDistance, arrivalRadius, cruiseSpeedPercent, slowSpeedPercent) ->
                    new ConfigureNavigationPacket(
                            pos,
                            (settings & 1) != 0,
                            (settings & 2) != 0,
                            avoidanceOffDistance,
                            arrivalRadius,
                            cruiseSpeedPercent,
                            slowSpeedPercent,
                            (settings & 4) != 0,
                            (settings & 8) != 0,
                            (settings & 16) != 0,
                            (settings & FLAG_INVERT_ANGLE) != 0,
                            (settings & FLAG_INVERT_THRUST) != 0,
                            (settings >> 8) & 0xFF,
                            (settings >> 16) & 0xFFFF
                    )
    );

    private final boolean activated;
    private final boolean debugOverlay;
    private final int avoidanceOffDistance;
    private final int arrivalRadius;
    private final int cruiseSpeedPercent;
    private final int slowSpeedPercent;
    private final boolean ignoreTerrain;
    private final boolean idleBraking;
    private final boolean helicopterMode;
    private final boolean invertAngle;
    private final boolean invertThrust;
    private final int helicopterMaxPitchDeg;
    private final int navMaxThrust;

    public ConfigureNavigationPacket(
            BlockPos pos,
            boolean activated,
            boolean debugOverlay,
            int avoidanceOffDistance,
            int arrivalRadius,
            int cruiseSpeedPercent,
            int slowSpeedPercent,
            boolean ignoreTerrain,
            boolean idleBraking,
            boolean helicopterMode,
            boolean invertAngle,
            boolean invertThrust,
            int helicopterMaxPitchDeg,
            int navMaxThrust
    ) {
        super(pos);
        this.activated = activated;
        this.debugOverlay = debugOverlay;
        this.avoidanceOffDistance = avoidanceOffDistance;
        this.arrivalRadius = arrivalRadius;
        this.cruiseSpeedPercent = cruiseSpeedPercent;
        this.slowSpeedPercent = slowSpeedPercent;
        this.ignoreTerrain = ignoreTerrain;
        this.idleBraking = idleBraking;
        this.helicopterMode = helicopterMode;
        this.invertAngle = invertAngle;
        this.invertThrust = invertThrust;
        this.helicopterMaxPitchDeg = helicopterMaxPitchDeg;
        this.navMaxThrust = navMaxThrust;
    }

    private static int packSettings(
            boolean activated,
            boolean debugOverlay,
            boolean ignoreTerrain,
            boolean idleBraking,
            boolean helicopterMode,
            boolean invertAngle,
            boolean invertThrust,
            int helicopterMaxPitchDeg,
            int navMaxThrust
    ) {
        int flags = 0;
        if (activated) flags |= 1;
        if (debugOverlay) flags |= 2;
        if (ignoreTerrain) flags |= 4;
        if (idleBraking) flags |= 8;
        if (helicopterMode) flags |= 16;
        if (invertAngle) flags |= FLAG_INVERT_ANGLE;
        if (invertThrust) flags |= FLAG_INVERT_THRUST;
        flags |= (Math.clamp(helicopterMaxPitchDeg, 0, NavigationSettings.MAX_HELICOPTER_PITCH_DEG) & 0xFF) << 8;
        flags |= (Math.clamp(navMaxThrust, 1, NavigationSettings.MAX_NAV_MAX_THRUST) & 0xFFFF) << 16;
        return flags;
    }

    @Override
    protected void applySettings(ServerPlayer player, NavigationBlockEntity be) {
        be.applyConfiguration(activated, debugOverlay, avoidanceOffDistance, arrivalRadius,
                cruiseSpeedPercent, slowSpeedPercent, ignoreTerrain, idleBraking,
                helicopterMode, invertAngle, invertThrust, helicopterMaxPitchDeg, navMaxThrust);
    }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return ModPackets.CONFIGURE_NAVIGATION;
    }
}
