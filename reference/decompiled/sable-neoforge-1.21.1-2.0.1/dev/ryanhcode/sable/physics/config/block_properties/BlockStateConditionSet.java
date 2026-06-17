package dev.ryanhcode.sable.physics.config.block_properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public record BlockStateConditionSet(List<BlockStateConditionSet.BlockStateCondition> blockStateConditions) {
   public static final Codec<BlockStateConditionSet> CODEC = Codec.STRING
      .comapFlatMap(BlockStateConditionSet::parse, BlockStateConditionSet::toString)
      .stable();

   public static DataResult<BlockStateConditionSet> parse(String value) {
      String[] parts = value.split(",");
      List<BlockStateConditionSet.BlockStateCondition> conditions = new ArrayList<>();

      try {
         for (String part : parts) {
            conditions.add(BlockStateConditionSet.BlockStateCondition.parse(part));
         }
      } catch (IllegalArgumentException var7) {
         return DataResult.error(var7::getMessage);
      }

      return DataResult.success(new BlockStateConditionSet(conditions));
   }

   @Override
   public String toString() {
      return String.join(",", this.blockStateConditions.stream().map(BlockStateConditionSet.BlockStateCondition::toString).toList());
   }

   public boolean matches(StateDefinition<Block, BlockState> stateDefinition, BlockState state) {
      for (BlockStateConditionSet.BlockStateCondition condition : this.blockStateConditions) {
         Property<?> property = stateDefinition.getProperty(condition.property());
         if (property == null) {
            return false;
         }

         Comparable<?> expectedValue = (Comparable<?>)property.getValue(condition.value()).orElse(null);
         if (expectedValue == null) {
            return false;
         }

         if (!state.getValue(property).equals(expectedValue)) {
            return false;
         }
      }

      return true;
   }

   public static record BlockStateCondition(String property, String value) {
      public static BlockStateConditionSet.BlockStateCondition parse(String value) {
         String[] parts = value.split("=");
         if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid block state condition: " + value);
         } else {
            return new BlockStateConditionSet.BlockStateCondition(parts[0], parts[1]);
         }
      }

      @Override
      public String toString() {
         return this.property + "=" + this.value;
      }
   }
}
