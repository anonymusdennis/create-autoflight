package net.createmod.catnip.net.base;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public class CatnipPacketRegistry {
   public final String modId;
   public final String networkVersion;
   private final Set<CatnipPacketRegistry.PacketType<?>> packets = new HashSet<>();
   public final Set<CatnipPacketRegistry.PacketType<?>> packetsView = Collections.unmodifiableSet(this.packets);
   private boolean packetsRegistered = false;

   public CatnipPacketRegistry(String modId, int networkVersion) {
      this(modId, String.valueOf(networkVersion));
   }

   public CatnipPacketRegistry(String modId, String networkVersion) {
      this.modId = modId;
      this.networkVersion = networkVersion;
   }

   public void registerPacket(CatnipPacketRegistry.PacketType<?> packetType) {
      if (this.packetsRegistered) {
         throw new IllegalStateException("Cannot register more packets after registerAllPackets() has been called!");
      } else {
         this.packets.add(packetType);
      }
   }

   public void registerAllPackets() {
      if (this.packetsRegistered) {
         throw new IllegalStateException("Cannot call registerAllPackets() more than once!");
      } else {
         CatnipServices.NETWORK.registerPackets(this);
         this.packetsRegistered = true;
      }
   }

   public static record PacketType<T extends BasePacketPayload>(Type<T> type, Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
   }
}
