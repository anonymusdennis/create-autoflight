package com.simibubi.create.api.stress;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.tterrag.registrate.util.nullness.NonNullConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.world.level.block.Block;

public class BlockStressValues {
   public static final SimpleRegistry<Block, DoubleSupplier> IMPACTS = SimpleRegistry.create();
   public static final SimpleRegistry<Block, DoubleSupplier> CAPACITIES = SimpleRegistry.create();
   public static final SimpleRegistry<Block, BlockStressValues.GeneratedRpm> RPM = SimpleRegistry.create();

   public static double getImpact(Block block) {
      DoubleSupplier supplier = IMPACTS.get(block);
      return supplier == null ? 0.0 : supplier.getAsDouble();
   }

   public static double getCapacity(Block block) {
      DoubleSupplier supplier = CAPACITIES.get(block);
      return supplier == null ? 0.0 : supplier.getAsDouble();
   }

   public static NonNullConsumer<Block> setGeneratorSpeed(int value) {
      return block -> RPM.register(block, new BlockStressValues.GeneratedRpm(value, false));
   }

   public static NonNullConsumer<Block> setGeneratorSpeed(int value, boolean mayGenerateLess) {
      return block -> RPM.register(block, new BlockStressValues.GeneratedRpm(value, mayGenerateLess));
   }

   private BlockStressValues() {
      throw new AssertionError("This class should not be instantiated");
   }

   public static record GeneratedRpm(int value, boolean mayGenerateLess) {
   }
}
