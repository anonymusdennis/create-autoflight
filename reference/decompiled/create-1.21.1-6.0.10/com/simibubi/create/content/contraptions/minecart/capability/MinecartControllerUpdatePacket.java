package com.simibubi.create.content.contraptions.minecart.capability;

import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MinecartControllerUpdatePacket(int entityId, @Nullable CompoundTag nbt) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, MinecartControllerUpdatePacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      MinecartControllerUpdatePacket::entityId,
      CatnipStreamCodecBuilders.nullable(ByteBufCodecs.COMPOUND_TAG),
      MinecartControllerUpdatePacket::nbt,
      MinecartControllerUpdatePacket::new
   );

   public MinecartControllerUpdatePacket(MinecartController controller, @NotNull Provider registries) {
      this(controller.cart().getId(), controller.isEmpty() ? null : controller.serializeNBT(registries));
   }

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      Entity entityByID = player.clientLevel.getEntity(this.entityId);
      if (entityByID != null) {
         if (entityByID.hasData(AllAttachmentTypes.MINECART_CONTROLLER)) {
            if (this.nbt == null) {
               entityByID.removeData(AllAttachmentTypes.MINECART_CONTROLLER);
            } else {
               MinecartController controller = (MinecartController)entityByID.getData(AllAttachmentTypes.MINECART_CONTROLLER);
               controller.deserializeNBT(player.registryAccess(), this.nbt);
            }
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.MINECART_CONTROLLER;
   }
}
