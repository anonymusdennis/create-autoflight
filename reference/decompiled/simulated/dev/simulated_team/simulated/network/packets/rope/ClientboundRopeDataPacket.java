package dev.simulated_team.simulated.network.packets.rope;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.util.SableBufferUtils;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import foundry.veil.api.network.handler.ClientPacketContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record ClientboundRopeDataPacket(
   int interpolationTick, BlockPos ownerPos, UUID uuid, List<Vector3d> points, @Nullable BlockPos startAttachmentPos, @Nullable BlockPos endAttachmentPos
) implements CustomPacketPayload {
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRopeDataPacket> CODEC = StreamCodec.of(
      (buf, value) -> value.write(buf), ClientboundRopeDataPacket::read
   );
   public static Type<ClientboundRopeDataPacket> TYPE = new Type(Simulated.path("rope_data"));

   private static ClientboundRopeDataPacket read(RegistryFriendlyByteBuf buf) {
      int interpolationTick = buf.readInt();
      BlockPos ownerPos = buf.readBlockPos();
      UUID uuid = buf.readUUID();
      int size = buf.readInt();
      List<Vector3d> points = new ObjectArrayList(size);

      for (int i = 0; i < size; i++) {
         points.add(SableBufferUtils.read(buf, new Vector3d()));
      }

      BlockPos startAttachment = buf.readBoolean() ? buf.readBlockPos() : null;
      BlockPos endAttachment = buf.readBoolean() ? buf.readBlockPos() : null;
      return new ClientboundRopeDataPacket(interpolationTick, ownerPos, uuid, points, startAttachment, endAttachment);
   }

   private void write(RegistryFriendlyByteBuf buf) {
      buf.writeInt(this.interpolationTick);
      buf.writeBlockPos(this.ownerPos);
      buf.writeUUID(this.uuid);
      buf.writeInt(this.points.size());

      for (Vector3dc point : this.points) {
         SableBufferUtils.write(buf, point);
      }

      buf.writeBoolean(this.startAttachmentPos != null);
      if (this.startAttachmentPos != null) {
         buf.writeBlockPos(this.startAttachmentPos);
      }

      buf.writeBoolean(this.endAttachmentPos != null);
      if (this.endAttachmentPos != null) {
         buf.writeBlockPos(this.endAttachmentPos);
      }
   }

   public void handle(ClientPacketContext context) {
      LocalPlayer player = context.player();
      Level level = player.level();
      if (level.getBlockEntity(this.ownerPos) instanceof SmartBlockEntity smartBlockEntity) {
         RopeStrandHolderBehavior ropeHolder = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(RopeStrandHolderBehavior.TYPE);
         if (ropeHolder != null) {
            ropeHolder.receiveClientStrand(this.interpolationTick, this.points, this.uuid, this.startAttachmentPos, this.endAttachmentPos);
         }
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
