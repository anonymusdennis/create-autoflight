package com.simibubi.create.content.equipment.sandPaper;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record SandPaperItemComponent(ItemStack item) {
   public static final Codec<SandPaperItemComponent> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(i -> i.item)).apply(instance, SandPaperItemComponent::new)
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, SandPaperItemComponent> STREAM_CODEC = StreamCodec.composite(
      ItemStack.OPTIONAL_STREAM_CODEC, i -> i.item, SandPaperItemComponent::new
   );

   @Override
   public final boolean equals(Object arg0) {
      if (arg0 instanceof ItemStack otherItem && ItemStack.isSameItemSameComponents(otherItem, this.item)) {
         return true;
      }

      return false;
   }

   @Override
   public final int hashCode() {
      return Objects.hash(this.item.getItem(), this.item.getCount(), this.item.getComponents());
   }
}
