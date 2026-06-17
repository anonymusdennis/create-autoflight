package com.simibubi.create.impl.effect;

import com.simibubi.create.api.effect.OpenPipeEffectHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

public class WaterEffectHandler implements OpenPipeEffectHandler {
   @Override
   public void apply(Level level, AABB area, FluidStack fluid) {
      if (level.getGameTime() % 5L == 0L) {
         for (Entity entity : level.getEntities((Entity)null, area, Entity::isOnFire)) {
            entity.clearFire();
         }

         BlockPos.betweenClosedStream(area).forEach(pos -> dowseFire(level, pos));
      }
   }

   private static void dowseFire(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.is(BlockTags.FIRE)) {
         level.removeBlock(pos, false);
      } else if (AbstractCandleBlock.isLit(state)) {
         AbstractCandleBlock.extinguish(null, state, level, pos);
      } else if (CampfireBlock.isLitCampfire(state)) {
         level.levelEvent(1009, pos, 0);
         CampfireBlock.dowse(null, level, pos, state);
         level.setBlockAndUpdate(pos, (BlockState)state.setValue(CampfireBlock.LIT, false));
      }
   }
}
