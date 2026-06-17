package com.simibubi.create.api.schematic.requirement;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SchematicRequirementRegistries {
   public static final SimpleRegistry<Block, SchematicRequirementRegistries.BlockRequirement> BLOCKS = SimpleRegistry.create();
   public static final SimpleRegistry<BlockEntityType<?>, SchematicRequirementRegistries.BlockEntityRequirement> BLOCK_ENTITIES = SimpleRegistry.create();
   public static final SimpleRegistry<EntityType<?>, SchematicRequirementRegistries.EntityRequirement> ENTITIES = SimpleRegistry.create();

   private SchematicRequirementRegistries() {
      throw new AssertionError("This class should not be instantiated");
   }

   @FunctionalInterface
   public interface BlockEntityRequirement {
      ItemRequirement getRequiredItems(BlockEntity var1, BlockState var2);
   }

   @FunctionalInterface
   public interface BlockRequirement {
      ItemRequirement getRequiredItems(BlockState var1, @Nullable BlockEntity var2);
   }

   @FunctionalInterface
   public interface EntityRequirement {
      ItemRequirement getRequiredItems(Entity var1);
   }
}
