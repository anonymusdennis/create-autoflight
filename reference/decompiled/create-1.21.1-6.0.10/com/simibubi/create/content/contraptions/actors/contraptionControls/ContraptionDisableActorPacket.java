package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import java.util.List;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ContraptionDisableActorPacket(int entityId, ItemStack filter, boolean enable) implements ClientboundPacketPayload {
   public static final StreamCodec<RegistryFriendlyByteBuf, ContraptionDisableActorPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      ContraptionDisableActorPacket::entityId,
      ItemStack.OPTIONAL_STREAM_CODEC,
      ContraptionDisableActorPacket::filter,
      ByteBufCodecs.BOOL,
      ContraptionDisableActorPacket::enable,
      ContraptionDisableActorPacket::new
   );

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      if (player.clientLevel.getEntity(this.entityId) instanceof AbstractContraptionEntity ace) {
         Contraption contraption = ace.getContraption();
         List disabledActors = contraption.getDisabledActors();
         if (this.filter.isEmpty()) {
            disabledActors.clear();
         }

         if (!this.enable) {
            disabledActors.add(this.filter);
            contraption.setActorsActive(this.filter, false);
         } else {
            disabledActors.removeIf(next -> ContraptionControlsMovement.isSameFilter(next, this.filter) || next.isEmpty());
            contraption.setActorsActive(this.filter, true);
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONTRAPTION_ACTOR_TOGGLE;
   }
}
