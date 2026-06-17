package com.simibubi.create.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.createmod.catnip.config.ConfigBase.ConfigInt;

public class CRecipes extends ConfigBase {
   public final ConfigBool bulkPressing = this.b(false, "bulkPressing", new String[]{CRecipes.Comments.bulkPressing});
   public final ConfigBool bulkCutting = this.b(false, "bulkCutting", new String[]{CRecipes.Comments.bulkCutting});
   public final ConfigBool allowBrewingInMixer = this.b(true, "allowBrewingInMixer", new String[]{CRecipes.Comments.allowBrewingInMixer});
   public final ConfigBool allowShapelessInMixer = this.b(true, "allowShapelessInMixer", new String[]{CRecipes.Comments.allowShapelessInMixer});
   public final ConfigBool allowShapedSquareInPress = this.b(true, "allowShapedSquareInPress", new String[]{CRecipes.Comments.allowShapedSquareInPress});
   public final ConfigBool allowRegularCraftingInCrafter = this.b(
      true, "allowRegularCraftingInCrafter", new String[]{CRecipes.Comments.allowRegularCraftingInCrafter}
   );
   public final ConfigInt maxFireworkIngredientsInCrafter = this.i(
      9, 1, "maxFireworkIngredientsInCrafter", new String[]{CRecipes.Comments.maxFireworkIngredientsInCrafter}
   );
   public final ConfigBool allowStonecuttingOnSaw = this.b(true, "allowStonecuttingOnSaw", new String[]{CRecipes.Comments.allowStonecuttingOnSaw});
   public final ConfigBool allowCastingBySpout = this.b(true, "allowCastingBySpout", new String[]{CRecipes.Comments.allowCastingBySpout});
   public final ConfigBool displayLogStrippingRecipes = this.b(true, "displayLogStrippingRecipes", new String[]{CRecipes.Comments.displayLogStrippingRecipes});
   public final ConfigInt lightSourceCountForRefinedRadiance = this.i(
      10, 1, "lightSourceCountForRefinedRadiance", new String[]{CRecipes.Comments.refinedRadiance}
   );
   public final ConfigBool enableRefinedRadianceRecipe = this.b(true, "enableRefinedRadianceRecipe", new String[]{CRecipes.Comments.refinedRadianceRecipe});
   public final ConfigBool enableShadowSteelRecipe = this.b(true, "enableShadowSteelRecipe", new String[]{CRecipes.Comments.shadowSteelRecipe});

   public String getName() {
      return "recipes";
   }

   private static class Comments {
      static String bulkPressing = "Allow the Mechanical Press to process entire stacks at a time.";
      static String bulkCutting = "Allow the Mechanical Saw to process entire stacks at a time.";
      static String allowBrewingInMixer = "Allow supported potions to be brewed by a Mechanical Mixer + Basin.";
      static String allowShapelessInMixer = "Allow any shapeless crafting recipes to be processed by a Mechanical Mixer + Basin.";
      static String allowShapedSquareInPress = "Allow any single-ingredient 2x2 or 3x3 crafting recipes to be processed by a Mechanical Press + Basin.";
      static String allowRegularCraftingInCrafter = "Allow any standard crafting recipes to be processed by Mechanical Crafters.";
      static String maxFireworkIngredientsInCrafter = "The Maximum amount of ingredients that can be used to craft Firework Rockets using Mechanical Crafters.";
      static String allowStonecuttingOnSaw = "Allow any stonecutting recipes to be processed by a Mechanical Saw.";
      static String allowWoodcuttingOnSaw = "Allow any wood related recipes to be processed by a Mechanical Saw.";
      static String allowCastingBySpout = "Allow Spouts to interact with Casting Tables and Basins from Tinkers' Construct.";
      static String refinedRadiance = "The amount of Light sources destroyed before Chromatic Compound turns into Refined Radiance.";
      static String refinedRadianceRecipe = "Allow the standard in-world Refined Radiance recipes.";
      static String shadowSteelRecipe = "Allow the standard in-world Shadow Steel recipe.";
      static String displayLogStrippingRecipes = "Display vanilla Log-stripping interactions in JEI.";
   }
}
