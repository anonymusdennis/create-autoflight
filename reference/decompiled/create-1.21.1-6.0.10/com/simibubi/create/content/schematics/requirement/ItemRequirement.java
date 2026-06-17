package com.simibubi.create.content.schematics.requirement;

import com.simibubi.create.api.schematic.requirement.SchematicRequirementRegistries;
import com.simibubi.create.api.schematic.requirement.SpecialBlockEntityItemRequirement;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.api.schematic.requirement.SpecialEntityItemRequirement;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.framedblocks.FramedBlocksInSchematics;
import com.simibubi.create.foundation.mixin.accessor.ItemFrameAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.createmod.catnip.components.ComponentProcessors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

public class ItemRequirement {
   public static final ItemRequirement NONE = new ItemRequirement(Collections.emptyList());
   public static final ItemRequirement INVALID = new ItemRequirement(Collections.emptyList());
   protected List<ItemRequirement.StackRequirement> requiredItems;

   public ItemRequirement(List<ItemRequirement.StackRequirement> requiredItems) {
      this.requiredItems = requiredItems;
   }

   public ItemRequirement(ItemRequirement.StackRequirement stackRequirement) {
      this(List.of(stackRequirement));
   }

   public ItemRequirement(ItemRequirement.ItemUseType usage, ItemStack stack) {
      this(new ItemRequirement.StackRequirement(stack, usage));
   }

   public ItemRequirement(ItemRequirement.ItemUseType usage, Item item) {
      this(usage, new ItemStack(item));
   }

   public ItemRequirement(ItemRequirement.ItemUseType usage, List<ItemStack> requiredItems) {
      this(requiredItems.stream().map(req -> new ItemRequirement.StackRequirement(req, usage)).collect(Collectors.toList()));
   }

   public static ItemRequirement of(BlockState state, @Nullable BlockEntity be) {
      Block block = state.getBlock();
      SchematicRequirementRegistries.BlockRequirement blockRequirement = SchematicRequirementRegistries.BLOCKS.get(block);
      ItemRequirement requirement;
      if (blockRequirement != null) {
         requirement = blockRequirement.getRequiredItems(state, be);
      } else if (block instanceof SpecialBlockItemRequirement specialBlock) {
         requirement = specialBlock.getRequiredItems(state, be);
      } else {
         requirement = defaultOf(state, be);
      }

      if (be != null) {
         SchematicRequirementRegistries.BlockEntityRequirement beRequirement = SchematicRequirementRegistries.BLOCK_ENTITIES.get(be.getType());
         if (beRequirement != null) {
            requirement = requirement.union(beRequirement.getRequiredItems(be, state));
         } else if (be instanceof SpecialBlockEntityItemRequirement specialBE) {
            requirement = requirement.union(specialBE.getRequiredItems(state));
         } else if (Mods.FRAMEDBLOCKS.contains(block)) {
            requirement = requirement.union(FramedBlocksInSchematics.getRequiredItems(state, be));
         }
      }

      return requirement;
   }

   private static ItemRequirement defaultOf(BlockState state, BlockEntity be) {
      Block block = state.getBlock();
      if (block == Blocks.AIR) {
         return NONE;
      } else {
         Item item = block.asItem();
         if (item == Items.AIR) {
            return INVALID;
         } else if (state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, new ItemStack(item, 2));
         } else if (block instanceof TurtleEggBlock) {
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, new ItemStack(item, (Integer)state.getValue(TurtleEggBlock.EGGS)));
         } else if (block instanceof SeaPickleBlock) {
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, new ItemStack(item, (Integer)state.getValue(SeaPickleBlock.PICKLES)));
         } else if (block instanceof SnowLayerBlock) {
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, new ItemStack(item, (Integer)state.getValue(SnowLayerBlock.LAYERS)));
         } else if (block == BuiltInRegistries.BLOCK.get(com.simibubi.create.foundation.data.recipe.Mods.FD.asResource("rich_soil_farmland"))) {
            return new ItemRequirement(
               ItemRequirement.ItemUseType.CONSUME,
               (Item)BuiltInRegistries.ITEM.get(com.simibubi.create.foundation.data.recipe.Mods.FD.asResource("rich_soil"))
            );
         } else if (block instanceof FarmBlock || block instanceof DirtPathBlock) {
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, Items.DIRT);
         } else if (block instanceof AbstractBannerBlock && be instanceof BannerBlockEntity bannerBE) {
            return new ItemRequirement(new ItemRequirement.StrictNbtStackRequirement(bannerBE.getItem(), ItemRequirement.ItemUseType.CONSUME));
         } else if (block == Blocks.TALL_GRASS) {
            return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, new ItemStack(Items.SHORT_GRASS, 2));
         } else {
            return block == Blocks.LARGE_FERN
               ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, new ItemStack(Items.FERN, 2))
               : new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, item);
         }
      }
   }

   public static ItemRequirement of(Entity entity) {
      SchematicRequirementRegistries.EntityRequirement requirement = SchematicRequirementRegistries.ENTITIES.get(entity.getType());
      if (requirement != null) {
         return requirement.getRequiredItems(entity);
      } else if (entity instanceof SpecialEntityItemRequirement specialEntity) {
         return specialEntity.getRequiredItems();
      } else if (entity instanceof ItemFrame itemFrame) {
         ItemStack frame = ((ItemFrameAccessor)itemFrame).create$getFrameItemStack();
         ItemStack displayedItem = ComponentProcessors.withUnsafeComponentsDiscarded(itemFrame.getItem());
         return displayedItem.isEmpty()
            ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, frame)
            : new ItemRequirement(
               List.of(
                  new ItemRequirement.StackRequirement(frame, ItemRequirement.ItemUseType.CONSUME),
                  new ItemRequirement.StrictNbtStackRequirement(displayedItem, ItemRequirement.ItemUseType.CONSUME)
               )
            );
      } else if (entity instanceof ArmorStand armorStand) {
         List<ItemRequirement.StackRequirement> requirements = new ArrayList<>();
         requirements.add(new ItemRequirement.StackRequirement(new ItemStack(Items.ARMOR_STAND), ItemRequirement.ItemUseType.CONSUME));
         armorStand.getAllSlots()
            .forEach(
               s -> requirements.add(
                     new ItemRequirement.StrictNbtStackRequirement(ComponentProcessors.withUnsafeComponentsDiscarded(s), ItemRequirement.ItemUseType.CONSUME)
                  )
            );
         return new ItemRequirement(requirements);
      } else {
         return INVALID;
      }
   }

   public boolean isEmpty() {
      return NONE == this;
   }

   public boolean isInvalid() {
      return INVALID == this;
   }

   public List<ItemRequirement.StackRequirement> getRequiredItems() {
      return this.requiredItems;
   }

   public ItemRequirement union(ItemRequirement other) {
      if (this.isInvalid() || other.isInvalid()) {
         return INVALID;
      } else if (this.isEmpty()) {
         return other;
      } else {
         return other.isEmpty()
            ? this
            : new ItemRequirement(Stream.concat(this.requiredItems.stream(), other.requiredItems.stream()).collect(Collectors.toList()));
      }
   }

   public static enum ItemUseType {
      CONSUME,
      DAMAGE;
   }

   public static class StackRequirement {
      public final ItemStack stack;
      public final ItemRequirement.ItemUseType usage;

      public StackRequirement(ItemStack stack, ItemRequirement.ItemUseType usage) {
         this.stack = stack;
         this.usage = usage;
      }

      public boolean matches(ItemStack other) {
         return ItemStack.isSameItem(this.stack, other);
      }
   }

   public static class StrictNbtStackRequirement extends ItemRequirement.StackRequirement {
      public StrictNbtStackRequirement(ItemStack stack, ItemRequirement.ItemUseType usage) {
         super(stack, usage);
      }

      @Override
      public boolean matches(ItemStack other) {
         return ItemStack.isSameItemSameComponents(this.stack, other);
      }
   }
}
