package com.simibubi.create.api.contraption.transformable;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.contraptions.StructureTransform;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class MovedBlockTransformerRegistries {
   public static final SimpleRegistry<Block, MovedBlockTransformerRegistries.BlockTransformer> BLOCK_TRANSFORMERS = SimpleRegistry.create();
   public static final SimpleRegistry<BlockEntityType<?>, MovedBlockTransformerRegistries.BlockEntityTransformer> BLOCK_ENTITY_TRANSFORMERS = SimpleRegistry.create();

   private MovedBlockTransformerRegistries() {
      throw new AssertionError("This class should not be instantiated");
   }

   @FunctionalInterface
   public interface BlockEntityTransformer {
      void transform(BlockEntity var1, StructureTransform var2);
   }

   @FunctionalInterface
   public interface BlockTransformer {
      BlockState transform(BlockState var1, StructureTransform var2);
   }
}
