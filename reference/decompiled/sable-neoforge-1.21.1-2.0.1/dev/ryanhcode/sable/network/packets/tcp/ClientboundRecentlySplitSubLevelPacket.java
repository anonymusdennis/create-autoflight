package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableBufferUtils;
import foundry.veil.api.network.handler.PacketContext;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record ClientboundRecentlySplitSubLevelPacket(UUID splitSubLevelID, UUID splitFromID, Pose3d pose) implements SableTCPPacket {
   public static Type<ClientboundRecentlySplitSubLevelPacket> TYPE = new Type(Sable.sablePath("recently_split_sub_level"));
   public static StreamCodec<RegistryFriendlyByteBuf, ClientboundRecentlySplitSubLevelPacket> CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      ClientboundRecentlySplitSubLevelPacket::splitSubLevelID,
      UUIDUtil.STREAM_CODEC,
      ClientboundRecentlySplitSubLevelPacket::splitFromID,
      SableBufferUtils.POSE3D_STREAM_CODEC,
      ClientboundRecentlySplitSubLevelPacket::pose,
      ClientboundRecentlySplitSubLevelPacket::new
   );

   @Override
   public void handle(PacketContext context) {
      SubLevelContainer container = SubLevelContainer.getContainer(context.level());
      if (container instanceof ClientSubLevelContainer clientContainer) {
         SubLevel subLevel = container.getSubLevel(this.splitSubLevelID);
         SubLevel splitFrom = container.getSubLevel(this.splitFromID);
         if (subLevel != null && splitFrom != null) {
            ((ClientSubLevel)subLevel).wasSplitFrom(clientContainer.getInterpolation(), (ClientSubLevel)splitFrom, this.pose);
         } else {
            Sable.LOGGER.error("Attempted to handle a recently split sub-level packet for a sub-level (or origin sub-level) that does not exist!");
         }
      }
   }

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
