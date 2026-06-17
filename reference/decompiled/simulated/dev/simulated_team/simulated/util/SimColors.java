package dev.simulated_team.simulated.util;

import java.awt.Color;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SimColors {
   public static int SUCCESS_LIME = new Color(158, 222, 115).getRGB();
   public static int NUH_UH_RED = new Color(255, 113, 113).getRGB();
   public static int REDSTONE_OFF = new Color(86, 1, 1).getRGB();
   public static int REDSTONE_ON = new Color(205, 0, 0).getRGB();
   public static int ADVANCABLE_GOLD = new Color(219, 162, 19).getRGB();
   public static int EPIC_OURPLE = new Color(165, 0, 170).getRGB();
   public static int STRESSED_RED = new Color(235, 50, 48).getRGB();
   public static int THROTTLE_VALUE_BROWN = new Color(68, 32, 0).getRGB();
   public static int ACTIVE_YELLOW = new Color(255, 235, 133).getRGB();
   public static int PERCHANCE_ORANGE = new Color(255, 201, 102).getRGB();
   public static int DISCARDABLE_ORANGE = new Color(255, 161, 102).getRGB();
   public static int TITLE_DARK_RED = new Color(89, 36, 36).getRGB();
   public static int GROSS_BINDING_BROWN = new Color(183, 60, 45).getRGB();
   public static int WOODEN_BROWN = new Color(142, 111, 73).getRGB();
   public static int OFF_WHITE = new Color(221, 221, 221).getRGB();
   public static int MEDIA_OURPLE = new Color(188, 118, 255).getRGB();

   public static int redstone(float frac) {
      return net.createmod.catnip.theme.Color.mixColors(REDSTONE_OFF, REDSTONE_ON, frac);
   }

   public static Color LChOklab(float lightness, float chroma, float hue) {
      double a = (double)chroma * Math.cos((double)hue);
      double b = (double)chroma * Math.sin((double)hue);
      return fromOklab(lightness, (float)a, (float)b);
   }

   public static Color fromOklab(float lightness, float a, float b) {
      float l_ = lightness + 0.39633778F * a + 0.21580376F * b;
      float m_ = lightness - 0.105561346F * a - 0.06385417F * b;
      float s_ = lightness - 0.08948418F * a - 1.2914855F * b;
      float l = l_ * l_ * l_;
      float m = m_ * m_ * m_;
      float s = s_ * s_ * s_;
      return new Color(
         Math.clamp(4.0767417F * l - 3.3077116F * m + 0.23096994F * s, 0.0F, 1.0F),
         Math.clamp(-1.268438F * l + 2.6097574F * m - 0.34131938F * s, 0.0F, 1.0F),
         Math.clamp(-0.0041960864F * l - 0.7034186F * m + 1.7076147F * s, 0.0F, 1.0F)
      );
   }

   public static Vector3d toOklab(Color c) {
      float l = 0.41222146F * (float)c.getRed() + 0.53633255F * (float)c.getGreen() + 0.051445995F * (float)c.getBlue();
      float m = 0.2119035F * (float)c.getRed() + 0.6806995F * (float)c.getGreen() + 0.10739696F * (float)c.getBlue();
      float s = 0.08830246F * (float)c.getRed() + 0.28171885F * (float)c.getGreen() + 0.6299787F * (float)c.getBlue();
      double l_ = Math.cbrt((double)(l / 255.0F));
      double m_ = Math.cbrt((double)(m / 255.0F));
      double s_ = Math.cbrt((double)(s / 255.0F));
      return new Vector3d(
         0.21045426F * l_ + 0.7936178F * m_ - 0.004072047F * s_,
         1.9779985F * l_ - 2.4285922F * m_ + 0.4505937F * s_,
         0.025904037F * l_ + 0.78277177F * m_ - 0.80867577F * s_
      );
   }

   public static Vector3d LabToLCh(Vector3dc Lab) {
      return new Vector3d(Lab.x(), Math.sqrt(Lab.y() * Lab.y() + Lab.z() * Lab.z()), Math.atan2(Lab.y(), Lab.z()));
   }
}
