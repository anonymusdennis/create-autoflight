package com.simibubi.create.content.logistics.item.filter.attribute.attributes;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.item.filter.attribute.AllItemAttributeTypes;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record EnchantAttribute(@Nullable Holder<Enchantment> enchantment) implements ItemAttribute {
   public static final MapCodec<EnchantAttribute> CODEC = Enchantment.CODEC.xmap(EnchantAttribute::new, EnchantAttribute::enchantment).fieldOf("value");
   public static final StreamCodec<RegistryFriendlyByteBuf, EnchantAttribute> STREAM_CODEC = Enchantment.STREAM_CODEC
      .map(EnchantAttribute::new, EnchantAttribute::enchantment);

   @Override
   public boolean appliesTo(ItemStack itemStack, Level level) {
      return EnchantmentHelper.getEnchantmentsForCrafting(itemStack).keySet().contains(this.enchantment);
   }

   @Override
   public String getTranslationKey() {
      return "has_enchant";
   }

   @Override
   public Object[] getTranslationParameters() {
      String parameter = "";
      if (this.enchantment != null) {
         parameter = ((Enchantment)this.enchantment.value()).description().getString();
      }

      return new Object[]{parameter};
   }

   @Override
   public ItemAttributeType getType() {
      return AllItemAttributeTypes.HAS_ENCHANT;
   }

   public static class Type implements ItemAttributeType {
      @NotNull
      @Override
      public ItemAttribute createAttribute() {
         return new EnchantAttribute(null);
      }

      @Override
      public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
         List<ItemAttribute> list = new ArrayList<>();

         for (Holder<Enchantment> enchantmentHolder : EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet()) {
            list.add(new EnchantAttribute(enchantmentHolder));
         }

         return list;
      }

      @Override
      public MapCodec<? extends ItemAttribute> codec() {
         return EnchantAttribute.CODEC;
      }

      @Override
      public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
         return EnchantAttribute.STREAM_CODEC;
      }
   }
}
