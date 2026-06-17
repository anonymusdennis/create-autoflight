package com.simibubi.create.api.packager.unpacking;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.impl.unpacking.DefaultUnpackingHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Experimental;

@Experimental
public interface UnpackingHandler {
   SimpleRegistry<Block, UnpackingHandler> REGISTRY = SimpleRegistry.create();
   UnpackingHandler DEFAULT = DefaultUnpackingHandler.INSTANCE;

   boolean unpack(Level var1, BlockPos var2, BlockState var3, Direction var4, List<ItemStack> var5, @Nullable PackageOrderWithCrafts var6, boolean var7);
}
