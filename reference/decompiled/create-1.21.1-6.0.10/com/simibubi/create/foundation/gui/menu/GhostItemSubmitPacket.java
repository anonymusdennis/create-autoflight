package com.simibubi.create.foundation.gui.menu;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryMenu;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record GhostItemSubmitPacket(ItemStack item, int slot) implements ServerboundPacketPayload {
   public static final StreamCodec<RegistryFriendlyByteBuf, GhostItemSubmitPacket> STREAM_CODEC = StreamCodec.composite(
      ItemStack.OPTIONAL_STREAM_CODEC, GhostItemSubmitPacket::item, ByteBufCodecs.INT, GhostItemSubmitPacket::slot, GhostItemSubmitPacket::new
   );

   public void handle(ServerPlayer player) {
      if (player.containerMenu instanceof GhostItemMenu<?> menu) {
         menu.ghostInventory.setStackInSlot(this.slot, this.item);
         menu.getSlot(36 + this.slot).setChanged();
      }

      if (player.containerMenu instanceof StockKeeperCategoryMenu menu && (this.item.isEmpty() || this.item.getItem() instanceof FilterItem)) {
         menu.proxyInventory.setStackInSlot(this.slot, this.item);
         menu.getSlot(36 + this.slot).setChanged();
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.SUBMIT_GHOST_ITEM;
   }
}
