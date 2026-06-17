package com.simibubi.create.api.contraption;

import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.contraptions.Contraption;
import java.util.function.Supplier;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.Nullable;

public final class ContraptionType {
   public final Supplier<? extends Contraption> factory;
   public final Reference<ContraptionType> holder = CreateBuiltInRegistries.CONTRAPTION_TYPE.createIntrusiveHolder(this);

   public ContraptionType(Supplier<? extends Contraption> factory) {
      this.factory = factory;
   }

   public boolean is(TagKey<ContraptionType> tag) {
      return this.holder.is(tag);
   }

   @Nullable
   public static Contraption fromType(String typeId) {
      ContraptionType legacy = AllContraptionTypes.BY_LEGACY_NAME.get(typeId);
      if (legacy != null) {
         return legacy.factory.get();
      } else {
         ResourceLocation id = ResourceLocation.tryParse(typeId);
         ContraptionType type = (ContraptionType)CreateBuiltInRegistries.CONTRAPTION_TYPE.get(id);
         return type == null ? null : type.factory.get();
      }
   }
}
