package com.simibubi.create.content.logistics.item.filter.attribute.attributes;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.item.filter.attribute.AllItemAttributeTypes;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InItemGroupAttribute implements ItemAttribute {
   public static final MapCodec<InItemGroupAttribute> CODEC = BuiltInRegistries.CREATIVE_MODE_TAB
      .byNameCodec()
      .xmap(InItemGroupAttribute::new, i -> i.group)
      .fieldOf("value");
   public static final StreamCodec<ByteBuf, InItemGroupAttribute> STREAM_CODEC = CatnipStreamCodecBuilders.nullable(ResourceLocation.STREAM_CODEC)
      .map(
         i -> new InItemGroupAttribute((CreativeModeTab)BuiltInRegistries.CREATIVE_MODE_TAB.get(i)),
         i -> i.group == null ? null : BuiltInRegistries.CREATIVE_MODE_TAB.getKey(i.group)
      );
   @Nullable
   private CreativeModeTab group;

   public InItemGroupAttribute(@Nullable CreativeModeTab group) {
      this.group = group;
   }

   private static boolean tabContainsItem(CreativeModeTab tab, ItemStack stack) {
      return tab.contains(stack) || tab.contains(new ItemStack(stack.getItem()));
   }

   @Override
   public boolean appliesTo(ItemStack stack, Level world) {
      if (this.group == null) {
         return false;
      } else {
         if (this.group.getDisplayItems().isEmpty() && this.group.getSearchTabDisplayItems().isEmpty()) {
            try {
               this.group.buildContents(new ItemDisplayParameters(world.enabledFeatures(), false, world.registryAccess()));
            } catch (LinkageError | RuntimeException var4) {
               Create.LOGGER.error("Attribute Filter: Item Group {} crashed while building contents.", this.group.getDisplayName().getString(), var4);
               this.group = null;
               return false;
            }
         }

         return tabContainsItem(this.group, stack);
      }
   }

   @Override
   public String getTranslationKey() {
      return "in_item_group";
   }

   @Override
   public Object[] getTranslationParameters() {
      return new Object[]{this.group == null ? "<none>" : this.group.getDisplayName().getString()};
   }

   @Override
   public ItemAttributeType getType() {
      return AllItemAttributeTypes.IN_ITEM_GROUP;
   }

   @Override
   public final boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return o instanceof InItemGroupAttribute that ? Objects.equals(this.group, that.group) : false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(this.group);
   }

   public static class Type implements ItemAttributeType {
      @NotNull
      @Override
      public ItemAttribute createAttribute() {
         return new InItemGroupAttribute(null);
      }

      @Override
      public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
         List<ItemAttribute> list = new ArrayList<>();

         for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            if (tab.getType() == net.minecraft.world.item.CreativeModeTab.Type.CATEGORY && InItemGroupAttribute.tabContainsItem(tab, stack)) {
               list.add(new InItemGroupAttribute(tab));
            }
         }

         return list;
      }

      @Override
      public MapCodec<? extends ItemAttribute> codec() {
         return InItemGroupAttribute.CODEC;
      }

      @Override
      public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
         return InItemGroupAttribute.STREAM_CODEC;
      }
   }
}
