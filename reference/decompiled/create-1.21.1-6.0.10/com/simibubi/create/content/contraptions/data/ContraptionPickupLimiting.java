package com.simibubi.create.content.contraptions.data;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.foundation.mixin.accessor.NbtAccounterAccessor;
import io.netty.buffer.Unpooled;
import net.minecraft.Util;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public class ContraptionPickupLimiting {
   public static final int NBT_LIMIT = 2097152;
   public static final int PACKET_FIXER_LIMIT = 209715200;
   public static final int XL_PACKETS_LIMIT = Integer.MAX_VALUE;
   public static final int BUFFER = 20000;
   public static final int LIMIT = (Integer)Util.make(() -> {
      if (Mods.PACKETFIXER.isLoaded()) {
         return 209715200;
      } else {
         return Mods.XLPACKETS.isLoaded() ? Integer.MAX_VALUE : 2097152;
      }
   }) - 20000;

   public static boolean isTooLargeForPickup(Tag data) {
      return nbtSize(data) > (long)LIMIT;
   }

   private static long nbtSize(Tag data) {
      FriendlyByteBuf test = new FriendlyByteBuf(Unpooled.buffer());
      test.writeNbt(data);
      NbtAccounter sizeTracker = NbtAccounter.unlimitedHeap();
      test.readNbt(sizeTracker);
      long size = ((NbtAccounterAccessor)sizeTracker).create$getUsage();
      test.release();
      return size;
   }
}
