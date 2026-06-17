package com.simibubi.create.impl.effect;

import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.fluids.FluidStack;

public class MilkEffectHandler implements OpenPipeEffectHandler {
   @Override
   public void apply(Level level, AABB area, FluidStack fluid) {
      if (level.getGameTime() % 5L == 0L) {
         for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAffectedByPotions)) {
            entity.removeEffectsCuredBy(EffectCures.MILK);
         }
      }
   }
}
