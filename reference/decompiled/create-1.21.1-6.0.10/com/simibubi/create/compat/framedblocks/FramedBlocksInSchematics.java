package com.simibubi.create.compat.framedblocks;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FramedBlocksInSchematics {
   static final List<String> KEYS_TO_RETAIN = List.of("intangible", "glowing", "reinforced", "camo", "camo_two");

   public static CompoundTag prepareBlockEntityData(BlockState blockState, BlockEntity blockEntity) {
      CompoundTag data = null;
      if (blockEntity == null) {
         return data;
      } else {
         data = blockEntity.saveWithFullMetadata(blockEntity.getLevel().registryAccess());
         List<String> keysToRemove = new ArrayList<>();

         for (String key : data.getAllKeys()) {
            if (!KEYS_TO_RETAIN.contains(key)) {
               keysToRemove.add(key);
            }
         }

         for (String keyx : keysToRemove) {
            data.remove(keyx);
         }

         if (data.getCompound("camo").contains("fluid")) {
            data.remove("camo");
         }

         if (data.getCompound("camo_two").contains("fluid")) {
            data.remove("camo_two");
         }

         return data;
      }
   }

   public static ItemRequirement getRequiredItems(BlockState blockState, BlockEntity blockEntity) {
      if (blockEntity == null) {
         return ItemRequirement.NONE;
      } else {
         CompoundTag data = blockEntity.saveWithFullMetadata(blockEntity.getLevel().registryAccess());
         List<ItemRequirement.StackRequirement> list = new ArrayList<>();
         if (data.getBoolean("intangible")) {
            list.add(new ItemRequirement.StackRequirement(new ItemStack(Items.PHANTOM_MEMBRANE), ItemRequirement.ItemUseType.CONSUME));
         }

         if (data.getBoolean("glowing")) {
            list.add(new ItemRequirement.StackRequirement(new ItemStack(Items.GLOWSTONE_DUST), ItemRequirement.ItemUseType.CONSUME));
         }

         if (data.getBoolean("reinforced")) {
            list.add(
               new ItemRequirement.StackRequirement(new ItemStack(Mods.FRAMEDBLOCKS.getItem("framed_reinforcement")), ItemRequirement.ItemUseType.CONSUME)
            );
         }

         if (data.contains("camo")) {
            addCamoStack(blockEntity.getLevel().holderLookup(Registries.BLOCK), data.getCompound("camo"), list);
         }

         if (data.contains("camo_two")) {
            addCamoStack(blockEntity.getLevel().holderLookup(Registries.BLOCK), data.getCompound("camo_two"), list);
         }

         return new ItemRequirement(list);
      }
   }

   private static void addCamoStack(HolderGetter<Block> level, CompoundTag tag, List<ItemRequirement.StackRequirement> list) {
      if (tag.contains("state")) {
         BlockState blockState = NbtUtils.readBlockState(level, tag.getCompound("state"));
         ItemStack itemStack = new ItemStack(blockState.getBlock());
         if (!itemStack.isEmpty()) {
            list.add(new ItemRequirement.StackRequirement(itemStack, ItemRequirement.ItemUseType.CONSUME));
         }
      }
   }
}
