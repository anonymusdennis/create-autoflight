package com.simibubi.create.infrastructure.data;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

public class TagLangGenerator {
   private final BiConsumer<String, String> output;

   public TagLangGenerator(BiConsumer<String, String> output) {
      this.output = output;
   }

   protected void translate(String key, String translation) {
      this.output.accept(key, translation);
   }

   protected void translate(TagKey<?> tag, String translation) {
      this.translate(keyFor(tag), translation);
   }

   private void translate(AllTags.AllBlockTags tag, String translation) {
      this.translate(tag.tag, translation);
   }

   private void translate(AllTags.AllItemTags tag, String translation) {
      this.translate(tag.tag, translation);
   }

   private void translate(AllTags.AllFluidTags tag, String translation) {
      this.translate(tag.tag, translation);
   }

   private void translate(AllTags.AllBlockTags block, AllTags.AllItemTags item, String translation) {
      this.translate(block, translation);
      this.translate(item, translation);
   }

   private void translate(AllTags.AllBlockTags block, AllTags.AllFluidTags fluid, String translation) {
      this.translate(block, translation);
      this.translate(fluid, translation);
   }

   private void translate(CommonMetal.ItemLikeTag tags, String translated) {
      this.translate(tags.blocks(), translated);
      this.translate(tags.items(), translated);
   }

   public void generate() {
      this.translate(AllTags.AllBlockTags.BRITTLE, "Brittle");
      this.translate(AllTags.AllBlockTags.CASING, "Casings");
      this.translate(AllTags.AllBlockTags.COPYCAT_ALLOW, "Copycat Copyable");
      this.translate(AllTags.AllBlockTags.COPYCAT_DENY, "Not Copycat Copyable");
      this.translate(AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_BLASTING, AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_BLASTING, "Blasting Catalysts");
      this.translate(AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_HAUNTING, AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_HAUNTING, "Haunting Catalysts");
      this.translate(AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_SMOKING, AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_SMOKING, "Smoking Catalysts");
      this.translate(AllTags.AllBlockTags.FAN_PROCESSING_CATALYSTS_SPLASHING, AllTags.AllFluidTags.FAN_PROCESSING_CATALYSTS_SPLASHING, "Splashing Catalysts");
      this.translate(AllTags.AllBlockTags.FAN_TRANSPARENT, "Fan Transparent");
      this.translate(AllTags.AllBlockTags.GIRDABLE_TRACKS, "Girdable Tracks");
      this.translate(AllTags.AllBlockTags.MOVABLE_EMPTY_COLLIDER, "Movable Empty Colliders");
      this.translate(AllTags.AllBlockTags.NON_MOVABLE, "Non-movable");
      this.translate(AllTags.AllBlockTags.NON_BREAKABLE, "Non-breakable");
      this.translate(AllTags.AllBlockTags.PASSIVE_BOILER_HEATERS, "Passive Boiler Heaters");
      this.translate(AllTags.AllBlockTags.SAFE_NBT, "Safe NBT");
      this.translate(AllTags.AllBlockTags.SEATS, AllTags.AllItemTags.SEATS, "Seats");
      this.translate(AllTags.AllBlockTags.POSTBOXES, AllTags.AllItemTags.POSTBOXES, "Postboxes");
      this.translate(AllTags.AllBlockTags.TABLE_CLOTHS, AllTags.AllItemTags.TABLE_CLOTHS, "Table Cloths");
      this.translate(AllTags.AllBlockTags.TOOLBOXES, AllTags.AllItemTags.TOOLBOXES, "Toolboxes");
      this.translate(AllTags.AllBlockTags.TRACKS, AllTags.AllItemTags.TRACKS, "Tracks");
      this.translate(AllTags.AllBlockTags.TREE_ATTACHMENTS, "Tree Attachments");
      this.translate(AllTags.AllBlockTags.VALVE_HANDLES, AllTags.AllItemTags.VALVE_HANDLES, "Valve Handles");
      this.translate(AllTags.AllBlockTags.WINDMILL_SAILS, "Windmill Sails");
      this.translate(AllTags.AllBlockTags.WRENCH_PICKUP, "Wrench-pickupable");
      this.translate(AllTags.AllBlockTags.CHEST_MOUNTED_STORAGE, "Mounted Chests");
      this.translate(AllTags.AllBlockTags.SIMPLE_MOUNTED_STORAGE, "Simple Mounted Storages");
      this.translate(AllTags.AllBlockTags.FALLBACK_MOUNTED_STORAGE_BLACKLIST, "Non-mountable Storages");
      this.translate(AllTags.AllBlockTags.ROOTS, "Roots");
      this.translate(AllTags.AllBlockTags.SUGAR_CANE_VARIANTS, "Sugarcane-like");
      this.translate(AllTags.AllBlockTags.NON_HARVESTABLE, "Non-harvestable");
      this.translate(AllTags.AllBlockTags.SINGLE_BLOCK_INVENTORIES, "Single-block Inventories");
      this.translate(AllTags.AllBlockTags.CARDBOARD_STORAGE_BLOCKS, AllTags.AllItemTags.CARDBOARD_STORAGE_BLOCKS, "Cardboard Storage Blocks");
      this.translate(AllTags.AllBlockTags.ANDESITE_ALLOY_STORAGE_BLOCKS, AllTags.AllItemTags.ANDESITE_ALLOY_STORAGE_BLOCKS, "Andesite Alloy Storage Blocks");
      this.translate(AllTags.AllBlockTags.CORALS, "Corals");
      this.translate(AllTags.AllItemTags.BLAZE_BURNER_FUEL_REGULAR, "Regular Blaze Burner Fuel");
      this.translate(AllTags.AllItemTags.BLAZE_BURNER_FUEL_SPECIAL, "Special Blaze Burner Fuel");
      this.translate(AllTags.AllItemTags.CASING, "Casings");
      this.translate(AllTags.AllItemTags.CONTRAPTION_CONTROLLED, "Contraption-controllable");
      this.translate(AllTags.AllItemTags.CREATE_INGOTS, "Create's Ingots");
      this.translate(AllTags.AllItemTags.CRUSHED_RAW_MATERIALS, "Crushed Raw Materials");
      this.translate(AllTags.AllItemTags.INVALID_FOR_TRACK_PAVING, "Track Paving Blacklist");
      this.translate(AllTags.AllItemTags.DEPLOYABLE_DRINK, "Deployable Drink");
      this.translate(AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES, "Pressurized Air Sources");
      this.translate(AllTags.AllItemTags.SANDPAPER, "Sandpaper");
      this.translate(AllTags.AllItemTags.DYED_TABLE_CLOTHS, "Dyed Table Cloths");
      this.translate(AllTags.AllItemTags.PULPIFIABLE, "Pulpifiable");
      this.translate(AllTags.AllItemTags.SLEEPERS, "Sleepers");
      this.translate(AllTags.AllItemTags.PACKAGES, "Packages");
      this.translate(AllTags.AllItemTags.CHAIN_RIDEABLE, "Can Ride Chains");
      this.translate(AllTags.AllItemTags.UPRIGHT_ON_BELT, "Upright on Belts");
      this.translate(AllTags.AllItemTags.NOT_UPRIGHT_ON_BELT, "Not Upright on Belts");
      this.translate(AllTags.AllItemTags.DISPENSE_BEHAVIOR_WRAP_BLACKLIST, "Dispense Behavior Wrap Blacklist");
      this.translate(AllTags.AllItemTags.OBSIDIAN_DUST, "Obsidian Dust");
      this.translate(AllTags.AllItemTags.PLATES, "Plates");
      this.translate(AllTags.AllItemTags.OBSIDIAN_PLATES, "Obsidian Plates");
      this.translate(AllTags.AllItemTags.CARDBOARD_PLATES, "Cardboard Plates");
      this.translate(AllTags.AllItemTags.CERTUS_QUARTZ, "Certus Quartz");
      this.translate(AllTags.AllItemTags.AMETRINE_ORES, "Ametrine Ores");
      this.translate(AllTags.AllItemTags.ANTHRACITE_ORES, "Anthracite Ores");
      this.translate(AllTags.AllItemTags.EMERALDITE_ORES, "Emeraldite Ores");
      this.translate(AllTags.AllItemTags.LIGNITE_ORES, "Lignite Ores");
      this.translate(AllTags.AllItemTags.HONEY_BUCKETS, "Honey Buckets");
      this.translate(AllTags.AllItemTags.FLOURS, "Flours");
      this.translate(AllTags.AllItemTags.WHEAT_FLOURS, "Wheat Flours");
      this.translate(AllTags.AllItemTags.FOODS_DOUGH_WHEAT, "Wheat Doughs");
      this.translate(AllTags.AllItemTags.UA_CORAL, "Upgrade Aquatic Coral");
      this.translate(AllTags.AllFluidTags.BOTTOMLESS_ALLOW, "Potentially Bottomless Fluids");
      this.translate(AllTags.AllFluidTags.BOTTOMLESS_DENY, "Non-bottomless Fluids");
      this.translate(AllTags.AllFluidTags.TEA, "Teas");
      this.translate(AllTags.AllFluidTags.CHOCOLATE, "Chocolate");
      this.translate(AllTags.AllFluidTags.CREOSOTE, "Creosote");
      this.translate(AllTags.AllRecipeSerializerTags.AUTOMATION_IGNORE.tag, "Non-automatable");
      this.translate(AllTags.AllContraptionTypeTags.OPENS_CONTROLS.tag, "Opens Contraption Controls");
      this.translate(AllTags.AllContraptionTypeTags.REQUIRES_VEHICLE_FOR_RENDER.tag, "Requires a Vehicle to Render");
      this.translate(AllTags.AllMountedItemStorageTypeTags.INTERNAL.tag, "Internal");
      this.translate(AllTags.AllMountedItemStorageTypeTags.FUEL_BLACKLIST.tag, "Doesn't Provide Fuel");
      this.translate(AllTags.AllItemTags.ALLURITE, "Allurite");
      this.translate(AllTags.AllItemTags.AMETHYST, "Amethyst");
      this.translate(AllTags.AllItemTags.LUMIERE, "Lumiere");

      for (AllPaletteStoneTypes type : AllPaletteStoneTypes.values()) {
         this.translate(type.materialTag, toWord(type.name()));
      }

      for (CommonMetal metal : CommonMetal.values()) {
         String name = toWord(metal.name);
         if (metal.isNatural) {
            this.translate(metal.ores, name + " Ores");
            this.translate(metal.rawOres, "Raw " + name + " Ores");
            this.translate(metal.rawStorageBlocks, "Raw " + name + " Storage Blocks");
         }

         this.translate(metal.ingots, name + " Ingots");
         this.translate(metal.storageBlocks, name + " Storage Blocks");
         this.translate(metal.nuggets, name + " Nuggets");
         this.translate(metal.plates, name + " Plates");
      }
   }

   protected static String keyFor(TagKey<?> tag) {
      ResourceLocation registryId = tag.registry().location();
      String registry = sanitize(registryId.getNamespace().equals("minecraft") ? registryId.getPath() : registryId.toLanguageKey());
      return "tag." + registry + "." + sanitize(tag.location().toLanguageKey());
   }

   private static String sanitize(String string) {
      return string.replace('/', '.');
   }

   protected static String toWord(String string) {
      if (string.isBlank()) {
         return string;
      } else {
         String lower = string.toLowerCase(Locale.ROOT);
         char first = Character.toUpperCase(lower.charAt(0));
         String rest = lower.substring(1);
         return first + rest;
      }
   }
}
