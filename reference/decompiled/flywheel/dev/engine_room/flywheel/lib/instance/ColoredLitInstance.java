package dev.engine_room.flywheel.lib.instance;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import net.minecraft.util.FastColor.ARGB32;

public abstract class ColoredLitInstance extends AbstractInstance implements FlatLit {
   public byte red = -1;
   public byte green = -1;
   public byte blue = -1;
   public byte alpha = -1;
   public int light = 0;

   public ColoredLitInstance(InstanceType<? extends ColoredLitInstance> type, InstanceHandle handle) {
      super(type, handle);
   }

   public ColoredLitInstance colorArgb(int argb) {
      return this.color(ARGB32.red(argb), ARGB32.green(argb), ARGB32.blue(argb), ARGB32.alpha(argb));
   }

   public ColoredLitInstance colorRgb(int rgb) {
      return this.color(ARGB32.red(rgb), ARGB32.green(rgb), ARGB32.blue(rgb));
   }

   public ColoredLitInstance color(int red, int green, int blue, int alpha) {
      return this.color((byte)red, (byte)green, (byte)blue, (byte)alpha);
   }

   public ColoredLitInstance color(int red, int green, int blue) {
      return this.color((byte)red, (byte)green, (byte)blue);
   }

   public ColoredLitInstance color(byte red, byte green, byte blue, byte alpha) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      this.alpha = alpha;
      return this;
   }

   public ColoredLitInstance color(byte red, byte green, byte blue) {
      this.red = red;
      this.green = green;
      this.blue = blue;
      return this;
   }

   public ColoredLitInstance color(float red, float green, float blue, float alpha) {
      return this.color((byte)((int)(red * 255.0F)), (byte)((int)(green * 255.0F)), (byte)((int)(blue * 255.0F)), (byte)((int)(alpha * 255.0F)));
   }

   public ColoredLitInstance color(float red, float green, float blue) {
      return this.color((byte)((int)(red * 255.0F)), (byte)((int)(green * 255.0F)), (byte)((int)(blue * 255.0F)));
   }

   public ColoredLitInstance light(int light) {
      this.light = light;
      return this;
   }
}
