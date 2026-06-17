package com.simibubi.create.content.redstone.thresholdSwitch;

import net.minecraft.network.chat.MutableComponent;

public interface ThresholdSwitchObservable {
   int getMaxValue();

   int getMinValue();

   int getCurrentValue();

   MutableComponent format(int var1);
}
