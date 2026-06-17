package com.simibubi.create.content.legacy;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.ItemStack;

public class ChromaticCompoundColor implements ItemColor {
   public int getColor(ItemStack stack, int layer) {
      Minecraft mc = Minecraft.getInstance();
      float pt = AnimationTickHolder.getPartialTicks();
      float progress = (float)((double)(mc.player.getViewYRot(pt) / 180.0F) * Math.PI) + AnimationTickHolder.getRenderTime() / 10.0F;
      if (layer == 0) {
         return Color.mixColors(ARGB32.color(110, 87, 115), ARGB32.color(107, 48, 116), (Mth.sin(progress) + 1.0F) / 2.0F);
      } else if (layer == 1) {
         return Color.mixColors(ARGB32.color(212, 93, 121), ARGB32.color(110, 87, 115), (Mth.sin((float)((double)progress + Math.PI)) + 1.0F) / 2.0F);
      } else {
         return layer == 2
            ? Color.mixColors(ARGB32.color(234, 144, 133), ARGB32.color(212, 93, 121), (Mth.sin((float)((double)(progress * 1.5F) + Math.PI)) + 1.0F) / 2.0F)
            : 0;
      }
   }
}
