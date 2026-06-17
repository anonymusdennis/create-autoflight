package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.client.DeltaTracker.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Timer.class})
public interface TimerAccessor {
   @Accessor
   float getDeltaTickResidual();
}
