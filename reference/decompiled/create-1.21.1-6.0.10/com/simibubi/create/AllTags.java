package com.simibubi.create;

import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.registry.CreateRegistries;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

public class AllTags {
   @Deprecated(
      since = "6.0.7",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public static <T> TagKey<T> optionalTag(Registry<T> registry, ResourceLocation id) {
      return TagKey.create(registry.key(), id);
   }

   @Deprecated(
      since = "6.0.7",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public static <T> TagKey<T> commonTag(Registry<T> registry, String path) {
      return optionalTag(registry, ResourceLocation.fromNamespaceAndPath("c", path));
   }

   @Deprecated(
      since = "6.0.7",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public static TagKey<Block> commonBlockTag(String path) {
      return commonTag(BuiltInRegistries.BLOCK, path);
   }

   @Deprecated(
      since = "6.0.7",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public static TagKey<Item> commonItemTag(String path) {
      return commonTag(BuiltInRegistries.ITEM, path);
   }

   @Deprecated(
      since = "6.0.7",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   public static TagKey<Fluid> commonFluidTag(String path) {
      return commonTag(BuiltInRegistries.FLUID, path);
   }

   public static enum AllBlockTags {
      BRITTLE,
      CASING,
      COPYCAT_ALLOW,
      COPYCAT_DENY,
      FAN_PROCESSING_CATALYSTS_BLASTING(AllTags.NameSpace.MOD, "fan_processing_catalysts/blasting"),
      FAN_PROCESSING_CATALYSTS_HAUNTING(AllTags.NameSpace.MOD, "fan_processing_catalysts/haunting"),
      FAN_PROCESSING_CATALYSTS_SMOKING(AllTags.NameSpace.MOD, "fan_processing_catalysts/smoking"),
      FAN_PROCESSING_CATALYSTS_SPLASHING(AllTags.NameSpace.MOD, "fan_processing_catalysts/splashing"),
      FAN_TRANSPARENT,
      GIRDABLE_TRACKS,
      MOVABLE_EMPTY_COLLIDER,
      NON_MOVABLE,
      NON_BREAKABLE,
      PASSIVE_BOILER_HEATERS,
      SAFE_NBT,
      SEATS,
      POSTBOXES,
      TABLE_CLOTHS,
      TOOLBOXES,
      TRACKS,
      TREE_ATTACHMENTS,
      VALVE_HANDLES,
      WINDMILL_SAILS,
      WRENCH_PICKUP,
      CHEST_MOUNTED_STORAGE,
      SIMPLE_MOUNTED_STORAGE,
      FALLBACK_MOUNTED_STORAGE_BLACKLIST,
      ROOTS,
      SUGAR_CANE_VARIANTS,
      NON_HARVESTABLE,
      SINGLE_BLOCK_INVENTORIES,
      PLOUGH_WHITELIST,
      PLOUGH_BLACKLIST,
      CARDBOARD_STORAGE_BLOCKS(AllTags.NameSpace.COMMON, "storage_blocks/cardboard"),
      ANDESITE_ALLOY_STORAGE_BLOCKS(AllTags.NameSpace.COMMON, "storage_blocks/andesite_alloy"),
      CORALS,
      SLIMY_LOGS(AllTags.NameSpace.TIC),
      NON_DOUBLE_DOOR(AllTags.NameSpace.QUARK);

      public final TagKey<Block> tag;

      private AllBlockTags() {
         this(AllTags.NameSpace.MOD);
      }

      private AllBlockTags(AllTags.NameSpace namespace) {
         this(namespace, null);
      }

      private AllBlockTags(AllTags.NameSpace namespace, @Nullable String pathOverride) {
         this.tag = TagKey.create(Registries.BLOCK, namespace.id(this, pathOverride));
      }

      public boolean matches(Block block) {
         return block.builtInRegistryHolder().is(this.tag);
      }

      public boolean matches(ItemStack stack) {
         return stack != null && stack.getItem() instanceof BlockItem blockItem && this.matches(blockItem.getBlock());
      }

      public boolean matches(BlockState state) {
         return state.is(this.tag);
      }
   }

   public static enum AllContraptionTypeTags {
      OPENS_CONTROLS,
      REQUIRES_VEHICLE_FOR_RENDER;

      public final TagKey<ContraptionType> tag;

      private AllContraptionTypeTags() {
         this(AllTags.NameSpace.MOD);
      }

      private AllContraptionTypeTags(AllTags.NameSpace namespace) {
         this(namespace, null);
      }

      private AllContraptionTypeTags(AllTags.NameSpace namespace, @Nullable String pathOverride) {
         this.tag = TagKey.create(CreateRegistries.CONTRAPTION_TYPE, namespace.id(this, pathOverride));
      }

      public boolean matches(ContraptionType type) {
         return type.is(this.tag);
      }
   }

   public static enum AllEntityTags {
      BLAZE_BURNER_CAPTURABLE,
      IGNORE_SEAT;

      public final TagKey<EntityType<?>> tag;

      private AllEntityTags() {
         this(AllTags.NameSpace.MOD);
      }

      private AllEntityTags(AllTags.NameSpace namespace) {
         this(namespace, null);
      }

      private AllEntityTags(AllTags.NameSpace namespace, @Nullable String pathOverride) {
         this.tag = TagKey.create(Registries.ENTITY_TYPE, namespace.id(this, pathOverride));
      }

      public boolean matches(EntityType<?> type) {
         return type.is(this.tag);
      }

      public boolean matches(Entity entity) {
         return this.matches(entity.getType());
      }
   }

   public static enum AllFluidTags {
      BOTTOMLESS_ALLOW(AllTags.NameSpace.MOD, "bottomless/allow"),
      BOTTOMLESS_DENY(AllTags.NameSpace.MOD, "bottomless/deny"),
      FAN_PROCESSING_CATALYSTS_BLASTING(AllTags.NameSpace.MOD, "fan_processing_catalysts/blasting"),
      FAN_PROCESSING_CATALYSTS_HAUNTING(AllTags.NameSpace.MOD, "fan_processing_catalysts/haunting"),
      FAN_PROCESSING_CATALYSTS_SMOKING(AllTags.NameSpace.MOD, "fan_processing_catalysts/smoking"),
      FAN_PROCESSING_CATALYSTS_SPLASHING(AllTags.NameSpace.MOD, "fan_processing_catalysts/splashing"),
      TEA(AllTags.NameSpace.COMMON),
      CHOCOLATE(AllTags.NameSpace.COMMON),
      CREOSOTE(AllTags.NameSpace.COMMON);

      public final TagKey<Fluid> tag;

      private AllFluidTags() {
         this(AllTags.NameSpace.MOD);
      }

      private AllFluidTags(AllTags.NameSpace namespace) {
         this(namespace, null);
      }

      private AllFluidTags(AllTags.NameSpace namespace, @Nullable String pathOverride) {
         this.tag = TagKey.create(Registries.FLUID, namespace.id(this, pathOverride));
      }

      public boolean matches(Fluid fluid) {
         return fluid.is(this.tag);
      }

      public boolean matches(FluidState state) {
         return state.is(this.tag);
      }
   }

   public static enum AllItemTags {
      @Deprecated(
         since = "6.0.7",
         forRemoval = true
      )
      @ScheduledForRemoval(
         inVersion = "1.21.1+ Port"
      )
      BLAZE_BURNER_FUEL_REGULAR(AllTags.NameSpace.MOD, "blaze_burner_fuel/regular"),
      @Deprecated(
         since = "6.0.7",
         forRemoval = true
      )
      @ScheduledForRemoval(
         inVersion = "1.21.1+ Port"
      )
      BLAZE_BURNER_FUEL_SPECIAL(AllTags.NameSpace.MOD, "blaze_burner_fuel/special"),
      CASING,
      CONTRAPTION_CONTROLLED,
      CREATE_INGOTS,
      CRUSHED_RAW_MATERIALS,
      INVALID_FOR_TRACK_PAVING,
      DEPLOYABLE_DRINK,
      PRESSURIZED_AIR_SOURCES,
      SANDPAPER,
      SEATS,
      POSTBOXES,
      TABLE_CLOTHS,
      DYED_TABLE_CLOTHS,
      PULPIFIABLE,
      SLEEPERS,
      TOOLBOXES,
      PACKAGES,
      CHAIN_RIDEABLE,
      TRACKS,
      UPRIGHT_ON_BELT,
      NOT_UPRIGHT_ON_BELT,
      NOT_POTION,
      VALVE_HANDLES,
      DISPENSE_BEHAVIOR_WRAP_BLACKLIST,
      OBSIDIAN_DUST(AllTags.NameSpace.COMMON, "dusts/obsidian"),
      PLATES(AllTags.NameSpace.COMMON),
      OBSIDIAN_PLATES(AllTags.NameSpace.COMMON, "plates/obsidian"),
      CARDBOARD_PLATES(AllTags.NameSpace.COMMON, "plates/cardboard"),
      ALLURITE(AllTags.NameSpace.MOD, "stone_types/galosphere/allurite"),
      AMETHYST(AllTags.NameSpace.MOD, "stone_types/galosphere/amethyst"),
      LUMIERE(AllTags.NameSpace.MOD, "stone_types/galosphere/lumiere"),
      CERTUS_QUARTZ(AllTags.NameSpace.COMMON, "gems/certus_quartz"),
      AMETRINE_ORES(AllTags.NameSpace.COMMON, "ores/ametrine"),
      ANTHRACITE_ORES(AllTags.NameSpace.COMMON, "ores/anthracite"),
      EMERALDITE_ORES(AllTags.NameSpace.COMMON, "ores/emeraldite"),
      LIGNITE_ORES(AllTags.NameSpace.COMMON, "ores/lignite"),
      CARDBOARD_STORAGE_BLOCKS(AllTags.NameSpace.COMMON, "storage_blocks/cardboard"),
      ANDESITE_ALLOY_STORAGE_BLOCKS(AllTags.NameSpace.COMMON, "storage_blocks/andesite_alloy"),
      CHOCOLATE_BUCKETS(AllTags.NameSpace.COMMON, "buckets/chocolate"),
      HONEY_BUCKETS(AllTags.NameSpace.COMMON, "buckets/honey"),
      FOODS_CHOCOLATE(AllTags.NameSpace.COMMON, "foods/chocolate"),
      DRINKS_TEA(AllTags.NameSpace.COMMON, "drinks/tea"),
      FLOURS(AllTags.NameSpace.COMMON),
      WHEAT_FLOURS(AllTags.NameSpace.COMMON, "flours/wheat"),
      FOODS_DOUGH_WHEAT(AllTags.NameSpace.COMMON, "foods/dough/wheat"),
      UA_CORAL(AllTags.NameSpace.MOD, "upgrade_aquatic/coral"),
      CURIOS_HEAD(AllTags.NameSpace.CURIOS, "head");

      public final TagKey<Item> tag;

      private AllItemTags() {
         this(AllTags.NameSpace.MOD);
      }

      private AllItemTags(AllTags.NameSpace namespace) {
         this(namespace, null);
      }

      private AllItemTags(AllTags.NameSpace namespace, @Nullable String pathOverride) {
         this.tag = TagKey.create(Registries.ITEM, namespace.id(this, pathOverride));
      }

      public boolean matches(Item item) {
         return item.builtInRegistryHolder().is(this.tag);
      }

      public boolean matches(ItemStack stack) {
         return stack.is(this.tag);
      }
   }

   public static enum AllMountedItemStorageTypeTags {
      INTERNAL,
      FUEL_BLACKLIST;

      public final TagKey<MountedItemStorageType<?>> tag;

      private AllMountedItemStorageTypeTags() {
         this(AllTags.NameSpace.MOD);
      }

      private AllMountedItemStorageTypeTags(AllTags.NameSpace namespace) {
         this(namespace, null);
      }

      private AllMountedItemStorageTypeTags(AllTags.NameSpace namespace, @Nullable String pathOverride) {
         this.tag = TagKey.create(CreateRegistries.MOUNTED_ITEM_STORAGE_TYPE, namespace.id(this, pathOverride));
      }

      public boolean matches(MountedItemStorage storage) {
         return this.matches(storage.type);
      }

      public boolean matches(MountedItemStorageType<?> type) {
         return type.is(this.tag);
      }
   }

   public static enum AllRecipeSerializerTags {
      AUTOMATION_IGNORE;

      public final TagKey<RecipeSerializer<?>> tag;

      private AllRecipeSerializerTags() {
         this(AllTags.NameSpace.MOD);
      }

      private AllRecipeSerializerTags(AllTags.NameSpace namespace) {
         this(namespace, null);
      }

      private AllRecipeSerializerTags(AllTags.NameSpace namespace, @Nullable String pathOverride) {
         this.tag = TagKey.create(Registries.RECIPE_SERIALIZER, namespace.id(this, pathOverride));
      }

      public boolean matches(RecipeSerializer<?> recipeSerializer) {
         ResourceKey<RecipeSerializer<?>> key = (ResourceKey<RecipeSerializer<?>>)BuiltInRegistries.RECIPE_SERIALIZER
            .getResourceKey(recipeSerializer)
            .orElseThrow();
         return ((Reference)BuiltInRegistries.RECIPE_SERIALIZER.getHolder(key).orElseThrow()).is(this.tag);
      }
   }

   public static enum NameSpace {
      MOD("create"),
      COMMON("c"),
      TIC("tconstruct"),
      QUARK("quark"),
      GS("galosphere"),
      CURIOS("curios");

      public final String id;

      private NameSpace(String id) {
         this.id = id;
      }

      public ResourceLocation id(String path) {
         return ResourceLocation.fromNamespaceAndPath(this.id, path);
      }

      public ResourceLocation id(Enum<?> entry, @Nullable String pathOverride) {
         return this.id(pathOverride != null ? pathOverride : Lang.asId(entry.name()));
      }
   }
}
