package com.simibubi.create.content.logistics.item.filter.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class SingletonItemAttribute implements ItemAttribute {
   private final SingletonItemAttribute.Type type;
   private final BiPredicate<ItemStack, Level> predicate;
   private final String translationKey;

   public SingletonItemAttribute(SingletonItemAttribute.Type type, BiPredicate<ItemStack, Level> predicate, String translationKey) {
      this.type = type;
      this.predicate = predicate;
      this.translationKey = translationKey;
   }

   @Override
   public boolean appliesTo(ItemStack stack, Level world) {
      return this.predicate.test(stack, world);
   }

   @Override
   public ItemAttributeType getType() {
      return this.type;
   }

   @Override
   public String getTranslationKey() {
      return this.translationKey;
   }

   public static final class Type implements ItemAttributeType {
      private final SingletonItemAttribute attribute;

      public Type(Function<SingletonItemAttribute.Type, SingletonItemAttribute> singletonFunc) {
         this.attribute = singletonFunc.apply(this);
      }

      @NotNull
      @Override
      public ItemAttribute createAttribute() {
         return this.attribute;
      }

      @Override
      public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
         return this.attribute.appliesTo(stack, level) ? List.of(this.attribute) : List.of();
      }

      @Override
      public MapCodec<? extends ItemAttribute> codec() {
         return Codec.unit(this.attribute).fieldOf("value");
      }

      @Override
      public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
         return StreamCodec.unit(this.attribute);
      }
   }
}
