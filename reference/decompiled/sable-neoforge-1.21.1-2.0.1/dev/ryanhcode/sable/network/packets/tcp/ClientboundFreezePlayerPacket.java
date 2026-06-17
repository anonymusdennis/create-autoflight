package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.util.SableBufferUtils;
import foundry.veil.api.network.handler.PacketContext;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record ClientboundFreezePlayerPacket(UUID subLevelID, Vector3dc localPosition) implements SableTCPPacket {
   public static final Type<ClientboundFreezePlayerPacket> TYPE = new Type(Sable.sablePath("freeze_player"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundFreezePlayerPacket> CODEC = StreamCodec.of(
      (buf, value) -> value.write(buf), ClientboundFreezePlayerPacket::read
   );

   private static ClientboundFreezePlayerPacket read(FriendlyByteBuf buf) {
      return new ClientboundFreezePlayerPacket(buf.readUUID(), SableBufferUtils.read(buf, new Vector3d()));
   }

   private void write(FriendlyByteBuf buf) {
      buf.writeUUID(this.subLevelID);
      SableBufferUtils.write(buf, this.localPosition);
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   @Override
   public void handle(PacketContext context) {
      Player player = context.player();

      assert player != null;

      ((PlayerFreezeExtension)player).sable$freezeTo(this.subLevelID, this.localPosition);
   }
}
