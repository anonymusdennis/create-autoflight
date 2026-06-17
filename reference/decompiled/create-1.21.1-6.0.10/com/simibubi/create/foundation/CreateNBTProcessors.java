package com.simibubi.create.foundation;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import java.util.List;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.nbt.NBTProcessors;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class CreateNBTProcessors {
   public static void register() {
      NBTProcessors.addProcessor(BlockEntityType.LECTERN, data -> {
         if (!data.contains("Book", 10)) {
            return data;
         } else {
            CompoundTag book = data.getCompound("Book");
            ResourceLocation writableBookResource = BuiltInRegistries.ITEM.getKey(Items.WRITABLE_BOOK);
            if (writableBookResource != BuiltInRegistries.ITEM.getDefaultKey() && book.getString("id").equals(writableBookResource.toString())) {
               return data;
            } else {
               WrittenBookContent bookContent = (WrittenBookContent)CatnipCodecUtils.decodeOrNull(WrittenBookContent.CODEC, book);
               if (bookContent == null) {
                  return data;
               } else {
                  for (Filterable<Component> page : bookContent.pages()) {
                     if (NBTProcessors.textComponentHasClickEvent((Component)page.get(false))) {
                        return null;
                     }
                  }

                  return data;
               }
            }
         }
      });
      NBTProcessors.addProcessor((BlockEntityType)AllBlockEntityTypes.CLIPBOARD.get(), CreateNBTProcessors::clipboardProcessor);
      NBTProcessors.addProcessor((BlockEntityType)AllBlockEntityTypes.CREATIVE_CRATE.get(), NBTProcessors.itemProcessor("Filter"));
   }

   public static CompoundTag clipboardProcessor(CompoundTag data) {
      DataComponentMap components = (DataComponentMap)CatnipCodecUtils.decodeOrNull(DataComponentMap.CODEC, data.getCompound("components"));
      if (components == null) {
         return data;
      } else {
         ClipboardContent content = (ClipboardContent)components.get(AllDataComponents.CLIPBOARD_CONTENT);
         if (content == null) {
            return data;
         } else {
            for (List<ClipboardEntry> entries : content.pages()) {
               for (ClipboardEntry entry : entries) {
                  if (NBTProcessors.textComponentHasClickEvent(entry.text)) {
                     return null;
                  }
               }
            }

            return data;
         }
      }
   }
}
