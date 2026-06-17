package com.simibubi.create.content.logistics.item.filter.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public interface ItemAttribute {
   Codec<ItemAttribute> CODEC = CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE.byNameCodec().dispatch(ItemAttribute::getType, ItemAttributeType::codec);
   StreamCodec<RegistryFriendlyByteBuf, ItemAttribute> STREAM_CODEC = ByteBufCodecs.registry(CreateRegistries.ITEM_ATTRIBUTE_TYPE)
      .dispatch(ItemAttribute::getType, ItemAttributeType::streamCodec);

   static CompoundTag saveStatic(ItemAttribute attribute, Provider registries) {
      CompoundTag nbt = new CompoundTag();
      nbt.put("attribute", (Tag)CatnipCodecUtils.encode(CODEC, registries, attribute).orElseThrow());
      return nbt;
   }

   @Nullable
   static ItemAttribute loadStatic(CompoundTag nbt, Provider registries) {
      return (ItemAttribute)CatnipCodecUtils.decodeOrNull(CODEC, registries, nbt.get("attribute"));
   }

   static List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
      List<ItemAttribute> attributes = new ArrayList<>();

      for (ItemAttributeType type : CreateBuiltInRegistries.ITEM_ATTRIBUTE_TYPE) {
         attributes.addAll(type.getAllAttributes(stack, level));
      }

      return attributes;
   }

   boolean appliesTo(ItemStack var1, Level var2);

   ItemAttributeType getType();

   @OnlyIn(Dist.CLIENT)
   default MutableComponent format(boolean inverted) {
      return CreateLang.translateDirect("item_attributes." + this.getTranslationKey() + (inverted ? ".inverted" : ""), this.getTranslationParameters());
   }

   String getTranslationKey();

   default Object[] getTranslationParameters() {
      return new String[0];
   }

   public static record ItemAttributeEntry(ItemAttribute attribute, boolean inverted) {
      public static final Codec<ItemAttribute.ItemAttributeEntry> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  ItemAttribute.CODEC.fieldOf("attribute").forGetter(ItemAttribute.ItemAttributeEntry::attribute),
                  Codec.BOOL.fieldOf("inverted").forGetter(ItemAttribute.ItemAttributeEntry::inverted)
               )
               .apply(i, ItemAttribute.ItemAttributeEntry::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, ItemAttribute.ItemAttributeEntry> STREAM_CODEC = StreamCodec.composite(
         ItemAttribute.STREAM_CODEC,
         ItemAttribute.ItemAttributeEntry::attribute,
         ByteBufCodecs.BOOL,
         ItemAttribute.ItemAttributeEntry::inverted,
         ItemAttribute.ItemAttributeEntry::new
      );
   }
}
