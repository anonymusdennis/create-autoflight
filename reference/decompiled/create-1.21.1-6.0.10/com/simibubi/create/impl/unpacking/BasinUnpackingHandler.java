package com.simibubi.create.impl.unpacking;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public enum BasinUnpackingHandler implements UnpackingHandler {
   INSTANCE;

   @Override
   public boolean unpack(
      Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate
   ) {
      if (level.getBlockEntity(pos) instanceof BasinBlockEntity basin) {
         basin.inputInventory.packagerMode = true;

         boolean var10;
         try {
            var10 = UnpackingHandler.DEFAULT.unpack(level, pos, state, side, items, orderContext, simulate);
         } finally {
            basin.inputInventory.packagerMode = false;
         }

         return var10;
      } else {
         return false;
      }
   }
}
