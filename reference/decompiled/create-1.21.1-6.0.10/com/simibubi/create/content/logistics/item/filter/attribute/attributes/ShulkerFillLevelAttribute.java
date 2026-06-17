package com.simibubi.create.content.logistics.item.filter.attribute.attributes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.item.filter.attribute.AllItemAttributeTypes;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ShulkerFillLevelAttribute(ShulkerFillLevelAttribute.ShulkerLevels levels) implements ItemAttribute {
   public static final MapCodec<ShulkerFillLevelAttribute> CODEC = ShulkerFillLevelAttribute.ShulkerLevels.CODEC
      .xmap(ShulkerFillLevelAttribute::new, ShulkerFillLevelAttribute::levels)
      .fieldOf("value");
   public static final StreamCodec<ByteBuf, ShulkerFillLevelAttribute> STREAM_CODEC = ShulkerFillLevelAttribute.ShulkerLevels.STREAM_CODEC
      .map(ShulkerFillLevelAttribute::new, ShulkerFillLevelAttribute::levels);

   @Override
   public boolean appliesTo(ItemStack stack, Level level) {
      return this.levels != null && this.levels.canApply(stack);
   }

   @Override
   public String getTranslationKey() {
      return "shulker_level";
   }

   @Override
   public Object[] getTranslationParameters() {
      String parameter = "";
      if (this.levels != null) {
         parameter = CreateLang.translateDirect("item_attributes." + this.getTranslationKey() + "." + this.levels.key).getString();
      }

      return new Object[]{parameter};
   }

   @Override
   public ItemAttributeType getType() {
      return AllItemAttributeTypes.SHULKER_FILL_LEVEL;
   }

   static enum ShulkerLevels implements StringRepresentable {
      EMPTY("empty", amount -> amount == 0),
      PARTIAL("partial", amount -> amount > 0 && amount < Integer.MAX_VALUE),
      FULL("full", amount -> amount == Integer.MAX_VALUE);

      public static final Codec<ShulkerFillLevelAttribute.ShulkerLevels> CODEC = StringRepresentable.fromValues(ShulkerFillLevelAttribute.ShulkerLevels::values);
      public static final StreamCodec<ByteBuf, ShulkerFillLevelAttribute.ShulkerLevels> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(
         ShulkerFillLevelAttribute.ShulkerLevels.class
      );
      private final Predicate<Integer> requiredSize;
      private final String key;

      private ShulkerLevels(String key, Predicate<Integer> requiredSize) {
         this.key = key;
         this.requiredSize = requiredSize;
      }

      @Nullable
      public static ShulkerFillLevelAttribute.ShulkerLevels fromKey(String key) {
         return Arrays.stream(values()).filter(shulkerLevels -> shulkerLevels.key.equals(key)).findFirst().orElse(null);
      }

      private static boolean isShulker(ItemStack stack) {
         return Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock;
      }

      public String getSerializedName() {
         return Lang.asId(this.name());
      }

      public boolean canApply(ItemStack testStack) {
         if (!isShulker(testStack)) {
            return false;
         } else {
            ItemContainerContents contents = (ItemContainerContents)testStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
            if (contents == ItemContainerContents.EMPTY) {
               return this.requiredSize.test(0);
            } else if (testStack.has(DataComponents.CONTAINER_LOOT)) {
               return false;
            } else if (contents.getSlots() > 0) {
               int rawSize = contents.getSlots();
               if (rawSize < 27) {
                  return this.requiredSize.test(rawSize);
               } else {
                  NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);
                  contents.copyInto(inventory);
                  boolean isFull = inventory.stream().allMatch(itemStack -> !itemStack.isEmpty() && itemStack.getCount() == itemStack.getMaxStackSize());
                  return this.requiredSize.test(isFull ? Integer.MAX_VALUE : rawSize);
               }
            } else {
               return this.requiredSize.test(0);
            }
         }
      }
   }

   public static class Type implements ItemAttributeType {
      @NotNull
      @Override
      public ItemAttribute createAttribute() {
         return new ShulkerFillLevelAttribute(null);
      }

      @Override
      public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
         List<ItemAttribute> list = new ArrayList<>();

         for (ShulkerFillLevelAttribute.ShulkerLevels shulkerLevels : ShulkerFillLevelAttribute.ShulkerLevels.values()) {
            if (shulkerLevels.canApply(stack)) {
               list.add(new ShulkerFillLevelAttribute(shulkerLevels));
            }
         }

         return list;
      }

      @Override
      public MapCodec<? extends ItemAttribute> codec() {
         return ShulkerFillLevelAttribute.CODEC;
      }

      @Override
      public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
         return ShulkerFillLevelAttribute.STREAM_CODEC;
      }
   }
}
