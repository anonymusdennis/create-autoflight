package com.simibubi.create.foundation.utility;

import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class FluidFormatter {
   public static String asString(long amount, boolean shorten) {
      Couple<MutableComponent> couple = asComponents(amount, shorten);
      return ((MutableComponent)couple.getFirst()).getString() + " " + ((MutableComponent)couple.getSecond()).getString();
   }

   public static Couple<MutableComponent> asComponents(long amount, boolean shorten) {
      return shorten && amount >= 1000L
         ? Couple.create(Component.literal(String.format("%.1f", (double)amount / 1000.0)), CreateLang.translateDirect("generic.unit.buckets"))
         : Couple.create(Component.literal(String.valueOf(amount)), CreateLang.translateDirect("generic.unit.millibuckets"));
   }
}
