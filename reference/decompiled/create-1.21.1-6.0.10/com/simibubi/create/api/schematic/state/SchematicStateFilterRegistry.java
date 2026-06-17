package com.simibubi.create.api.schematic.state;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SchematicStateFilterRegistry {
   public static final SimpleRegistry<Block, SchematicStateFilterRegistry.StateFilter> REGISTRY = SimpleRegistry.create();

   private SchematicStateFilterRegistry() {
      throw new AssertionError("This class should not be instantiated");
   }

   @FunctionalInterface
   public interface StateFilter {
      BlockState filterStates(@Nullable BlockEntity var1, BlockState var2);
   }
}
