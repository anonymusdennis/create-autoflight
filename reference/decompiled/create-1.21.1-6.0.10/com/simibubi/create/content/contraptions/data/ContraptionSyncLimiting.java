package com.simibubi.create.content.contraptions.data;

import com.simibubi.create.compat.Mods;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class ContraptionSyncLimiting {
   public static final int SIZE_LIMIT = 1048576;
   public static final int PACKET_FIXER_LIMIT = 104857600;
   public static final int XL_PACKETS_LIMIT = Integer.MAX_VALUE;
   public static final int BUFFER = 20000;
   public static final int LIMIT = (Integer)Util.make(() -> {
      if (Mods.PACKETFIXER.isLoaded()) {
         return 104857600;
      } else {
         return Mods.XLPACKETS.isLoaded() ? Integer.MAX_VALUE : 1048576;
      }
   }) - 20000;

   public static void writeSafe(CompoundTag compound, FriendlyByteBuf dst) {
      int writerIndexBefore = dst.writerIndex();
      dst.writeNbt(compound);
      if (dst.writerIndex() > LIMIT) {
         dst.writerIndex(writerIndexBefore);
         dst.writeNbt(null);
      }
   }
}
