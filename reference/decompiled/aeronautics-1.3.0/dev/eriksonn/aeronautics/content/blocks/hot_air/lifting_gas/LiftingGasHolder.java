package dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.index.AeroRegistries;

public record LiftingGasHolder(LiftingGasType type, LiftingGasData data) {
   public static Codec<LiftingGasHolder> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               Aeronautics.getRegistrate().byNameCodecExpanded(AeroRegistries.Keys.LIFTING_GAS_TYPE).fieldOf("type").forGetter(LiftingGasHolder::type),
               LiftingGasData.CODEC.fieldOf("data").forGetter(LiftingGasHolder::data)
            )
            .apply(instance, LiftingGasHolder::new)
   );

   @Override
   public boolean equals(Object o) {
      return !(o instanceof LiftingGasHolder) ? false : this.type == ((LiftingGasHolder)o).type;
   }
}
