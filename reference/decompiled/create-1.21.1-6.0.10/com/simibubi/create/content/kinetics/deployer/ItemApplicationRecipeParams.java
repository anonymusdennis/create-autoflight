package com.simibubi.create.content.kinetics.deployer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class ItemApplicationRecipeParams extends ProcessingRecipeParams {
   public static MapCodec<ItemApplicationRecipeParams> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               codec(ItemApplicationRecipeParams::new).forGetter(Function.identity()),
               Codec.BOOL.optionalFieldOf("keep_held_item", false).forGetter(ItemApplicationRecipeParams::keepHeldItem)
            )
            .apply(instance, (params, keepHeldItem) -> {
               params.keepHeldItem = keepHeldItem;
               return params;
            })
   );
   public static StreamCodec<RegistryFriendlyByteBuf, ItemApplicationRecipeParams> STREAM_CODEC = streamCodec(ItemApplicationRecipeParams::new);
   protected boolean keepHeldItem;

   protected final boolean keepHeldItem() {
      return this.keepHeldItem;
   }

   @Override
   protected void encode(RegistryFriendlyByteBuf buffer) {
      super.encode(buffer);
      ByteBufCodecs.BOOL.encode(buffer, this.keepHeldItem);
   }

   @Override
   protected void decode(RegistryFriendlyByteBuf buffer) {
      super.decode(buffer);
      this.keepHeldItem = (Boolean)ByteBufCodecs.BOOL.decode(buffer);
   }
}
