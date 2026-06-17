package dev.createautoflight.network;

import dev.createautoflight.CreateAutoflight;
import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public enum ModPackets implements BasePacketPayload.PacketTypeProvider {
    CONFIGURE_GYROSCOPE(ConfigureGyroscopePacket.class, ConfigureGyroscopePacket.STREAM_CODEC),
    CONFIGURE_THRUSTER(ConfigureThrusterPacket.class, ConfigureThrusterPacket.STREAM_CODEC),
    CONFIGURE_NAVIGATION(ConfigureNavigationPacket.class, ConfigureNavigationPacket.STREAM_CODEC),
    CONFIGURE_THRUST_GEARBOX(ConfigureThrustGearboxPacket.class, ConfigureThrustGearboxPacket.STREAM_CODEC),
    CONFIGURE_THRUST_CONTROLLER(ConfigureThrustControllerPacket.class, ConfigureThrustControllerPacket.STREAM_CODEC);

    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> ModPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateAutoflight.MOD_ID, name().toLowerCase());
        this.type = new CatnipPacketRegistry.PacketType<>(
                new CustomPacketPayload.Type<>(id),
                clazz,
                codec
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) type.type();
    }

    public static void register() {
        CatnipPacketRegistry registry = new CatnipPacketRegistry(CreateAutoflight.MOD_ID, "1");
        for (ModPackets packet : values()) {
            registry.registerPacket(packet.type);
        }
        registry.registerAllPackets();
    }
}
