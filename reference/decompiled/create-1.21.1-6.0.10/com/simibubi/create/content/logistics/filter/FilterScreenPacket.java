package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record FilterScreenPacket(FilterScreenPacket.Option option, @Nullable CompoundTag data) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, FilterScreenPacket> STREAM_CODEC = StreamCodec.composite(
      FilterScreenPacket.Option.STREAM_CODEC,
      FilterScreenPacket::option,
      CatnipStreamCodecBuilders.nullable(ByteBufCodecs.COMPOUND_TAG),
      FilterScreenPacket::data,
      FilterScreenPacket::new
   );

   public FilterScreenPacket(FilterScreenPacket.Option option) {
      this(option, null);
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONFIGURE_FILTER;
   }

   public void handle(ServerPlayer player) {
      CompoundTag tag = this.data == null ? new CompoundTag() : this.data;
      if (player.containerMenu instanceof FilterMenu c) {
         if (this.option == FilterScreenPacket.Option.WHITELIST) {
            c.blacklist = false;
         }

         if (this.option == FilterScreenPacket.Option.BLACKLIST) {
            c.blacklist = true;
         }

         if (this.option == FilterScreenPacket.Option.RESPECT_DATA) {
            c.respectNBT = true;
         }

         if (this.option == FilterScreenPacket.Option.IGNORE_DATA) {
            c.respectNBT = false;
         }

         if (this.option == FilterScreenPacket.Option.UPDATE_FILTER_ITEM) {
            c.ghostInventory.setStackInSlot(tag.getInt("Slot"), ItemStack.parseOptional(player.registryAccess(), tag.getCompound("Item")));
         }
      }

      if (player.containerMenu instanceof AttributeFilterMenu c) {
         if (this.option == FilterScreenPacket.Option.WHITELIST) {
            c.whitelistMode = AttributeFilterWhitelistMode.WHITELIST_DISJ;
         }

         if (this.option == FilterScreenPacket.Option.WHITELIST2) {
            c.whitelistMode = AttributeFilterWhitelistMode.WHITELIST_CONJ;
         }

         if (this.option == FilterScreenPacket.Option.BLACKLIST) {
            c.whitelistMode = AttributeFilterWhitelistMode.BLACKLIST;
         }

         if (this.option == FilterScreenPacket.Option.ADD_TAG) {
            c.appendSelectedAttribute(ItemAttribute.loadStatic(this.data, player.registryAccess()), false);
         }

         if (this.option == FilterScreenPacket.Option.ADD_INVERTED_TAG) {
            c.appendSelectedAttribute(ItemAttribute.loadStatic(this.data, player.registryAccess()), true);
         }
      }

      if (player.containerMenu instanceof PackageFilterMenu c && this.option == FilterScreenPacket.Option.UPDATE_ADDRESS) {
         c.address = tag.getString("Address");
      }
   }

   public static enum Option {
      WHITELIST,
      WHITELIST2,
      BLACKLIST,
      RESPECT_DATA,
      IGNORE_DATA,
      UPDATE_FILTER_ITEM,
      ADD_TAG,
      ADD_INVERTED_TAG,
      UPDATE_ADDRESS;

      public static final StreamCodec<ByteBuf, FilterScreenPacket.Option> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(FilterScreenPacket.Option.class);
   }
}
