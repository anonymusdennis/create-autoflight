package dev.engine_room.flywheel.lib.math;

import net.minecraft.util.Mth;

public final class DataPacker {
   private DataPacker() {
   }

   public static byte packNormU8(float f) {
      return (byte)((int)(Mth.clamp(f, 0.0F, 1.0F) * 255.0F));
   }

   public static float unpackNormU8(byte b) {
      return (float)Byte.toUnsignedInt(b) / 255.0F;
   }

   public static byte packNormI8(float f) {
      return (byte)((int)(Mth.clamp(f, -1.0F, 1.0F) * 127.0F));
   }

   public static float unpackNormI8(byte b) {
      return (float)b / 127.0F;
   }

   public static short packNormU16(float f) {
      return (short)((int)(Mth.clamp(f, 0.0F, 1.0F) * 65535.0F));
   }

   public static float unpackNormU16(short s) {
      return (float)Short.toUnsignedInt(s) / 65535.0F;
   }

   public static short packNormI16(float f) {
      return (short)((int)(Mth.clamp(f, -1.0F, 1.0F) * 32767.0F));
   }

   public static float unpackNormI16(short s) {
      return (float)s / 32767.0F;
   }
}
