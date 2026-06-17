package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.CuttingRecipeGen;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public final class CreateCuttingRecipeGen extends CuttingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe ANDESITE_ALLOY = this.create(
      CreateRecipeProvider.I::andesiteAlloy,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(200)).output((ItemLike)AllBlocks.SHAFT.get(), 6)
   );
   BaseRecipeProvider.GeneratedRecipe BAMBOO_PLANKS = this.create(
      () -> Blocks.BAMBOO_PLANKS, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(20)).output(Blocks.BAMBOO_MOSAIC, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ARS_N_1 = this.stripAndMakePlanks(Mods.ARS_N, null, "stripped_purple_archwood_log", "archwood_planks");
   BaseRecipeProvider.GeneratedRecipe ARS_N_2 = this.stripAndMakePlanks(Mods.ARS_N, null, "stripped_green_archwood_log", "archwood_planks");
   BaseRecipeProvider.GeneratedRecipe ARS_N_3 = this.stripAndMakePlanks(Mods.ARS_N, null, "stripped_red_archwood_log", "archwood_planks");
   BaseRecipeProvider.GeneratedRecipe ARS_N_4 = this.stripAndMakePlanks(Mods.ARS_N, null, "stripped_purple_archwood_wood", "archwood_planks");
   BaseRecipeProvider.GeneratedRecipe ARS_N_5 = this.stripAndMakePlanks(Mods.ARS_N, null, "stripped_green_archwood_wood", "archwood_planks");
   BaseRecipeProvider.GeneratedRecipe ARS_N_6 = this.stripAndMakePlanks(Mods.ARS_N, null, "stripped_red_archwood_wood", "archwood_planks");
   BaseRecipeProvider.GeneratedRecipe ARS_E_1 = this.stripAndMakePlanksDiffPlanksModId(
      Mods.ARS_E, null, "stripped_yellow_archwood_log", Mods.ARS_N, "archwood_planks"
   );
   BaseRecipeProvider.GeneratedRecipe ARS_E_2 = this.stripAndMakePlanksDiffPlanksModId(
      Mods.ARS_E, null, "stripped_yellow_archwood", Mods.ARS_N, "archwood_planks"
   );
   BaseRecipeProvider.GeneratedRecipe RU_1 = this.stripAndMakePlanks(Mods.RU, "brimwood_log_magma", "stripped_brimwood_log", null);
   BaseRecipeProvider.GeneratedRecipe RU_2 = this.stripAndMakePlanks(Mods.RU, "ashen_log", "stripped_dead_log", null);
   BaseRecipeProvider.GeneratedRecipe RU_3 = this.stripAndMakePlanks(Mods.RU, "ashen_wood", "stripped_dead_wood", null);
   BaseRecipeProvider.GeneratedRecipe RU_4 = this.stripOnlyDiffModId(Mods.RU, "silver_birch_log", Mods.MC, "stripped_birch_log");
   BaseRecipeProvider.GeneratedRecipe RU_5 = this.stripOnlyDiffModId(Mods.RU, "silver_birch_wood", Mods.MC, "stripped_birch_wood");
   BaseRecipeProvider.GeneratedRecipe AUTUM_1 = this.stripAndMakePlanks(Mods.AUTUM, null, "sappy_maple_log", "maple_planks");
   BaseRecipeProvider.GeneratedRecipe AUTUM_2 = this.stripAndMakePlanks(Mods.AUTUM, null, "sappy_maple_wood", "maple_planks");
   BaseRecipeProvider.GeneratedRecipe ENDERGETIC_1 = this.stripAndMakePlanks(Mods.ENDER, "glowing_poise_stem", "stripped_poise_stem", null);
   BaseRecipeProvider.GeneratedRecipe ENDERGETIC_2 = this.stripAndMakePlanks(Mods.ENDER, "glowing_poise_wood", "stripped_poise_wood", null);
   BaseRecipeProvider.GeneratedRecipe IE_WIRES = this.ieWires(
      CommonMetal.COPPER, CommonMetal.ELECTRUM, CommonMetal.ALUMINUM, CommonMetal.STEEL, CommonMetal.LEAD
   );
   BaseRecipeProvider.GeneratedRecipe JNE_1 = this.stripAndMakePlanks(Mods.JNE, "cerebrage_claret_stem", "stripped_claret_stem", null);
   BaseRecipeProvider.GeneratedRecipe JNE_2 = this.stripAndMakePlanks(Mods.JNE, "cerebrage_claret_hyphae", "stripped_claret_hyphae", null);
   BaseRecipeProvider.GeneratedRecipe ATM_1 = this.stripAndMakePlanks(Mods.ATM, "watchful_aspen_log", "aspen_log", null);
   BaseRecipeProvider.GeneratedRecipe ATM_2 = this.stripAndMakePlanks(Mods.ATM, "watchful_aspen_wood", "aspen_wood", null);
   BaseRecipeProvider.GeneratedRecipe ATM_3 = this.stripAndMakePlanks(Mods.ATM, "crustose_log", "aspen_log", null);
   BaseRecipeProvider.GeneratedRecipe ATM_4 = this.stripAndMakePlanks(Mods.ATM, "crustose_wood", "aspen_wood", null);
   BaseRecipeProvider.GeneratedRecipe BWG_1 = this.stripAndMakePlanksDiffPlanksModId(Mods.BWG, null, "stripped_palo_verde_log", Mods.VANILLA, "birch_planks");
   BaseRecipeProvider.GeneratedRecipe BWG_2 = this.stripAndMakePlanksDiffPlanksModId(Mods.BWG, null, "stripped_palo_verde_wood", Mods.VANILLA, "birch_planks");

   public CreateCuttingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }

   BaseRecipeProvider.GeneratedRecipe ieWires(CommonMetal... metals) {
      for (CommonMetal metal : metals) {
         this.create(
            Mods.IE.recipeId("wire_" + metal),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(metal.plates))
                     .output(1.0F, Mods.IE, "wire_" + metal, 2))
                  .whenModLoaded(Mods.IE.getId())
         );
      }

      return null;
   }
}
