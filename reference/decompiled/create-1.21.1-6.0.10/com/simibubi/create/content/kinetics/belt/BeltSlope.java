package com.simibubi.create.content.kinetics.belt;

import net.createmod.catnip.lang.Lang;
import net.minecraft.util.StringRepresentable;

public enum BeltSlope implements StringRepresentable {
   HORIZONTAL,
   UPWARD,
   DOWNWARD,
   VERTICAL,
   SIDEWAYS;

   public String getSerializedName() {
      return Lang.asId(this.name());
   }

   public boolean isDiagonal() {
      return this == UPWARD || this == DOWNWARD;
   }
}
