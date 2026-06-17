package com.simibubi.create.api.data.recipe;

import net.minecraft.resources.ResourceLocation;

public interface DatagenMod {
   default ResourceLocation asResource(String id) {
      return ResourceLocation.fromNamespaceAndPath(this.getId(), id);
   }

   default String recipeId(String id) {
      return "compat/" + this.getId() + "/" + id;
   }

   String getId();

   default ResourceLocation ingotOf(String type) {
      return ResourceLocation.fromNamespaceAndPath(this.getId(), this.reversedMetalPrefix() ? "ingot_" + type : type + "_ingot");
   }

   default ResourceLocation nuggetOf(String type) {
      return ResourceLocation.fromNamespaceAndPath(this.getId(), this.reversedMetalPrefix() ? "nugget_" + type : type + "_nugget");
   }

   default ResourceLocation oreOf(String type) {
      return ResourceLocation.fromNamespaceAndPath(this.getId(), this.reversedMetalPrefix() ? "ore_" + type : type + "_ore");
   }

   default ResourceLocation deepslateOreOf(String type) {
      return ResourceLocation.fromNamespaceAndPath(this.getId(), this.reversedMetalPrefix() ? "deepslate_ore_" + type : "deepslate_" + type + "_ore");
   }

   default boolean reversedMetalPrefix() {
      return false;
   }

   default boolean strippedIsSuffix() {
      return false;
   }

   default boolean omitWoodSuffix() {
      return false;
   }
}
