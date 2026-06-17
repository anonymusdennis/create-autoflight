package net.createmod.catnip.nbt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.components.ComponentProcessors;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class NBTProcessors {
   private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> processors = new HashMap<>();
   private static final Map<BlockEntityType<?>, UnaryOperator<CompoundTag>> survivalProcessors = new HashMap<>();
   private static final UnaryOperator<CompoundTag> signProcessor = data -> {
      for (String key : List.of("front_text", "back_text")) {
         SignText text = CatnipCodecUtils.<SignText>decode(SignText.DIRECT_CODEC, data.getCompound(key)).orElse(null);
         if (text != null) {
            for (Component component : text.getMessages(false)) {
               if (textComponentHasClickEvent(component)) {
                  return null;
               }
            }
         }
      }

      return !data.contains("front_item") && !data.contains("back_item") ? data : null;
   };

   public static synchronized void addProcessor(BlockEntityType<?> type, UnaryOperator<CompoundTag> processor) {
      processors.put(type, processor);
   }

   public static synchronized void addSurvivalProcessor(BlockEntityType<?> type, UnaryOperator<CompoundTag> processor) {
      survivalProcessors.put(type, processor);
   }

   public static UnaryOperator<CompoundTag> itemProcessor(String tagKey) {
      return data -> {
         CompoundTag compound = data.getCompound(tagKey);
         if (!compound.contains("components", 10)) {
            return data;
         } else {
            CompoundTag itemComponents = compound.getCompound("components");

            for (String key : new HashSet(itemComponents.getAllKeys())) {
               DataComponentType<?> type = (DataComponentType<?>)BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(key));
               if (type != null && ComponentProcessors.isUnsafeItemComponent(type)) {
                  itemComponents.remove(key);
               }
            }

            if (itemComponents.isEmpty()) {
               compound.remove("components");
            }

            return data;
         }
      };
   }

   public static boolean textComponentHasClickEvent(Component component) {
      for (Component sibling : component.getSiblings()) {
         if (textComponentHasClickEvent(sibling)) {
            return true;
         }
      }

      return component.getStyle().getClickEvent() != null;
   }

   private NBTProcessors() {
   }

   @Nullable
   public static CompoundTag process(BlockState state, BlockEntity blockEntity, @Nullable CompoundTag compound, boolean survival) {
      if (compound == null) {
         return null;
      } else {
         BlockEntityType<?> type = blockEntity.getType();
         if (survival && survivalProcessors.containsKey(type)) {
            compound = survivalProcessors.get(type).apply(compound);
         }

         if (compound != null && processors.containsKey(type)) {
            return processors.get(type).apply(compound);
         } else if (blockEntity instanceof SpawnerBlockEntity) {
            return compound;
         } else if (state.is(BlockTags.ALL_SIGNS)) {
            return signProcessor.apply(compound);
         } else {
            return blockEntity.onlyOpCanSetNbt() ? null : compound;
         }
      }
   }
}
