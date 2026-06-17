package net.createmod.catnip.animation;

import net.minecraft.util.Mth;

public class AnimationFunctions {
   public static float easeOut(float t) {
      return Mth.sin((float) (Math.PI / 2) * t);
   }

   public static float easeInOut(float t) {
      return (float)Math.pow((double)Mth.sin((float) (Math.PI / 2) * t), 2.0);
   }

   public static float easeIn(float t) {
      return (float)Math.pow((double)t, 1.7);
   }
}
