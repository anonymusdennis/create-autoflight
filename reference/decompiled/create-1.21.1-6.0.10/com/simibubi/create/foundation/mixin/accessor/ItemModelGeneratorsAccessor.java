package com.simibubi.create.foundation.mixin.accessor;

import java.util.List;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.ItemModelGenerators.TrimModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ItemModelGenerators.class})
public interface ItemModelGeneratorsAccessor {
   @Accessor("GENERATED_TRIM_MODELS")
   static List<TrimModelData> create$getGENERATED_TRIM_MODELS() {
      throw new AssertionError();
   }
}
