package dev.createautoflight.network;

import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import dev.createautoflight.content.gyroscope.GyroscopeBlockEntity;
import dev.createautoflight.content.navigation.GyroTargetAngles;
import dev.createautoflight.content.navigation.NavigationSettings;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.createmod.catnip.net.base.BasePacketPayload;

public class ConfigureGyroscopePacket extends BlockEntityConfigurationPacket<GyroscopeBlockEntity> {
    public static final StreamCodec<ByteBuf, ConfigureGyroscopePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, p -> p.pos,
            ByteBufCodecs.INT, p -> p.mode,
            ByteBufCodecs.BOOL, p -> p.autoStabilize,
            ByteBufCodecs.INT, p -> p.forcePercent,
            ByteBufCodecs.INT, p -> p.axisFlags,
            ByteBufCodecs.INT, p -> p.targetPacked,
            ConfigureGyroscopePacket::new
    );

    private final int mode;
    private final boolean autoStabilize;
    private final int forcePercent;
    private final int axisFlags;
    private final int targetPacked;

    public ConfigureGyroscopePacket(
            BlockPos pos,
            int mode,
            boolean autoStabilize,
            int forcePercent,
            int dampingPercent,
            int acceptAngleDeg,
            boolean stabilizePitch,
            boolean stabilizeYaw,
            boolean stabilizeRoll,
            boolean bidirectionalTorque,
            int downFace,
            int targetPitchDeg,
            int targetYawDeg,
            int targetRollDeg
    ) {
        super(pos);
        this.mode = mode;
        this.autoStabilize = autoStabilize;
        this.forcePercent = forcePercent;
        this.axisFlags = packAxisFlags(
                stabilizePitch, stabilizeYaw, stabilizeRoll, bidirectionalTorque, dampingPercent, acceptAngleDeg, downFace
        );
        this.targetPacked = GyroTargetAngles.pack(targetPitchDeg, targetYawDeg, targetRollDeg);
    }

    private ConfigureGyroscopePacket(
            BlockPos pos,
            int mode,
            boolean autoStabilize,
            int forcePercent,
            int axisFlags,
            int targetPacked
    ) {
        super(pos);
        this.mode = mode;
        this.autoStabilize = autoStabilize;
        this.forcePercent = forcePercent;
        this.axisFlags = axisFlags;
        this.targetPacked = targetPacked;
    }

    @Override
    protected void applySettings(ServerPlayer player, GyroscopeBlockEntity be) {
        GyroTargetAngles targets = GyroTargetAngles.unpack(targetPacked);
        int pitch = be.isNavTargetOverride() ? be.getTargetPitchDeg() : (int) targets.pitchDeg();
        int yaw = be.isNavTargetOverride() ? be.getTargetYawDeg() : (int) targets.yawDeg();
        int roll = be.isNavTargetOverride() ? be.getTargetRollDeg() : (int) targets.rollDeg();
        be.applyConfiguration(
                GyroscopeBlockEntity.GyroMode.values()[Math.clamp(mode, 0, 1)],
                autoStabilize,
                forcePercent,
                (axisFlags >> 8) & 0xFF,
                (axisFlags >> 16) & 0xFF,
                (axisFlags & 1) != 0,
                (axisFlags & 2) != 0,
                (axisFlags & 4) != 0,
                (axisFlags & 8) != 0,
                Direction.from3DDataValue(Math.clamp((axisFlags >> 24) & 7, 0, 5)),
                pitch,
                yaw,
                roll
        );
    }

    @Override
    public BasePacketPayload.PacketTypeProvider getTypeProvider() {
        return ModPackets.CONFIGURE_GYROSCOPE;
    }

    public static int packAxisFlags(
            boolean pitch,
            boolean yaw,
            boolean roll,
            boolean bidirectional,
            int dampingPercent,
            int acceptAngleDeg,
            int downFace
    ) {
        int flags = 0;
        if (pitch) flags |= 1;
        if (yaw) flags |= 2;
        if (roll) flags |= 4;
        if (bidirectional) flags |= 8;
        flags |= (Math.clamp(dampingPercent, 0, GyroscopeBlockEntity.MAX_DAMPING_PERCENT) << 8);
        flags |= (Math.clamp(acceptAngleDeg, 0, 45) << 16);
        flags |= (Math.clamp(downFace, 0, 5) << 24);
        return flags;
    }
}
