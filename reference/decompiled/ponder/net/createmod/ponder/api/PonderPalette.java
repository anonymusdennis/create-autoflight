package net.createmod.ponder.api;

import net.createmod.catnip.theme.Color;

public enum PonderPalette {
   WHITE(15658734),
   BLACK(2232593),
   RED(16735596),
   GREEN(9222737),
   BLUE(6253743),
   SLOW(2293538),
   MEDIUM(34047),
   FAST(16733695),
   INPUT(8375776),
   OUTPUT(14532966);

   private final Color color;

   private PonderPalette(int color) {
      this.color = new Color(color, false).setImmutable();
   }

   public int getColor() {
      return this.color.getRGB();
   }

   public Color getColorObject() {
      return this.color;
   }
}
