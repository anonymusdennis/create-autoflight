package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.data.recipe.Mods;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;

public abstract class CuttingRecipeGen extends StandardProcessingRecipeGen<CuttingRecipe> {
   protected BaseRecipeProvider.GeneratedRecipe stripAndMakePlanks(Block wood, Block stripped, Block planks) {
      return this.stripAndMakePlanks(wood, stripped, planks, 6);
   }

   protected BaseRecipeProvider.GeneratedRecipe stripAndMakePlanks(Block wood, Block stripped, Block planks, int planksAmount) {
      this.create(() -> wood, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(stripped));
      return this.create(() -> stripped, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(planks, planksAmount));
   }

   protected BaseRecipeProvider.GeneratedRecipe cuttingCompat(DatagenMod mod, String... woodtypes) {
      for (String type : woodtypes) {
         String planks = type + "_planks";
         if (mod == Mods.ARS_N && type.contains("archwood")) {
            planks = "archwood_planks";
         }

         String strippedPre = mod.strippedIsSuffix() ? "" : "stripped_";
         String strippedPost = mod.strippedIsSuffix() ? "_stripped" : "";
         this.stripAndMakePlanks(mod, type + "_log", strippedPre + type + "_log" + strippedPost, planks);
         String wood = type + (mod.omitWoodSuffix() ? "" : "_wood");
         this.stripAndMakePlanks(mod, wood, strippedPre + wood + strippedPost, planks);
      }

      return null;
   }

   protected BaseRecipeProvider.GeneratedRecipe cuttingCompatLogOnly(DatagenMod mod, String... woodtypes) {
      for (String type : woodtypes) {
         String planks = type + "_planks";
         String strippedPre = mod.strippedIsSuffix() ? "" : "stripped_";
         String strippedPost = mod.strippedIsSuffix() ? "_stripped" : "";
         this.stripAndMakePlanks(mod, type + "_log", strippedPre + type + "_log" + strippedPost, planks);
      }

      return null;
   }

   protected BaseRecipeProvider.GeneratedRecipe stripOnlyDiffModId(DatagenMod mod1, String wood, DatagenMod mod2, String stripped) {
      this.create(
         "compat/" + mod1.getId() + "/" + wood,
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(mod1, wood))
                  .output(1.0F, mod2, stripped, 1))
               .whenModLoaded(mod1.getId())
      );
      return null;
   }

   protected BaseRecipeProvider.GeneratedRecipe stripAndMakePlanksDiffPlanksModId(DatagenMod mod1, String log, String stripped, DatagenMod mod2, String planks) {
      if (log != null) {
         this.create(
            "compat/" + mod1.getId() + "/" + log,
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(mod1, log))
                     .output(1.0F, mod1, stripped, 1))
                  .whenModLoaded(mod1.getId())
         );
      }

      if (planks != null) {
         this.create(
            "compat/" + mod1.getId() + "/" + stripped,
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(mod1, stripped))
                     .output(1.0F, mod2, planks, 6))
                  .whenModLoaded(mod1.getId())
         );
      }

      return null;
   }

   protected BaseRecipeProvider.GeneratedRecipe stripAndMakePlanks(DatagenMod mod, String wood, String stripped, String planks) {
      if (wood != null) {
         this.create(
            "compat/" + mod.getId() + "/" + wood,
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(mod, wood))
                     .output(1.0F, mod, stripped, 1))
                  .whenModLoaded(mod.getId())
         );
      }

      if (planks != null) {
         if (!Objects.equals(mod.getId(), Mods.VH.getId())) {
            this.create(
               "compat/" + mod.getId() + "/" + stripped,
               b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(mod, stripped))
                        .output(1.0F, mod, planks, 6))
                     .whenModLoaded(mod.getId())
            );
         } else {
            this.create(
               "compat/" + mod.getId() + "/" + stripped,
               b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(mod, stripped))
                        .output(1.0F, mod, planks, 4))
                     .whenModLoaded(mod.getId())
            );
         }
      }

      return null;
   }

   public CuttingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.CUTTING;
   }
}
