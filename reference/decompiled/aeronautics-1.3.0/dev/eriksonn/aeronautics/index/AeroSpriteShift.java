package dev.eriksonn.aeronautics.index;

import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import dev.eriksonn.aeronautics.Aeronautics;

public class AeroSpriteShift {
   public static final CTSpriteShiftEntry LEVITITE = omni("block/levitite");
   public static final CTSpriteShiftEntry PEARLESCENT_LEVITITE = omni("block/pearlescent_levitite");

   static CTSpriteShiftEntry omni(String name) {
      return CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL, Aeronautics.path(name), Aeronautics.path(name + "_connected"));
   }
}
