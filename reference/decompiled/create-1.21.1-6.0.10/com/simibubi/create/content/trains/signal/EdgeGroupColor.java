package com.simibubi.create.content.trains.signal;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.theme.Color;
import net.minecraft.network.codec.StreamCodec;

public enum EdgeGroupColor {
   YELLOW(15450709),
   GREEN(5357652),
   BLUE(5476833),
   ORANGE(14904886),
   LAVENDER(13341370),
   RED(10761528),
   CYAN(7264985),
   BROWN(10583128),
   WHITE(15065564);

   public static final StreamCodec<ByteBuf, EdgeGroupColor> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(EdgeGroupColor.class);
   private final Color color;
   private final int mask;

   private EdgeGroupColor(int color) {
      this.color = new Color(color);
      this.mask = 1 << this.ordinal();
   }

   public int strikeFrom(int mask) {
      return this == WHITE ? mask : mask | this.mask;
   }

   public Color get() {
      return this.color;
   }

   public static EdgeGroupColor getDefault() {
      return values()[0];
   }

   public static EdgeGroupColor findNextAvailable(int mask) {
      EdgeGroupColor[] values = values();

      for (EdgeGroupColor value : values) {
         if ((mask & 1) == 0) {
            return value;
         }

         mask >>= 1;
      }

      return WHITE;
   }
}
