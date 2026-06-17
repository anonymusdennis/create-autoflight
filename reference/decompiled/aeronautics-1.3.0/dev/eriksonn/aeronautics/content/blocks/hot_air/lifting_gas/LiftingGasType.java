package dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas;

import net.minecraft.network.chat.Component;

public interface LiftingGasType {
   Component getName();

   double getFillingTime();

   double getEmptyingTime();

   double getLiftStrength();

   double getResponsivenessAdjustmentFactor();

   double getResponsivenessAdjustmentRange();
}
