package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.FillingRecipeGen;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.common.Tags.Fluids;

public final class CreateFillingRecipeGen extends FillingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe HONEY_BOTTLE = this.create(
      "honey_bottle",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Fluids.HONEY, 250))
               .require(Items.GLASS_BOTTLE))
            .output(Items.HONEY_BOTTLE)
   );
   BaseRecipeProvider.GeneratedRecipe BUILDERS_TEA = this.create(
      "builders_tea",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  (FlowingFluid)AllFluids.TEA.get(), 250
               ))
               .require(Items.GLASS_BOTTLE))
            .output((ItemLike)AllItems.BUILDERS_TEA.get())
   );
   BaseRecipeProvider.GeneratedRecipe FD_MILK = this.create(
      Mods.FD.recipeId("milk_bottle"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     Fluids.MILK, 250
                  ))
                  .require(Items.GLASS_BOTTLE))
               .output(1.0F, Mods.FD, "milk_bottle", 1))
            .whenModLoaded(Mods.FD.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BLAZE_CAKE = this.create(
      "blaze_cake",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  net.minecraft.world.level.material.Fluids.LAVA, 250
               ))
               .require((ItemLike)AllItems.BLAZE_CAKE_BASE.get()))
            .output((ItemLike)AllItems.BLAZE_CAKE.get())
   );
   BaseRecipeProvider.GeneratedRecipe HONEYED_APPLE = this.create(
      "honeyed_apple",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Fluids.HONEY, 250))
               .require(Items.APPLE))
            .output((ItemLike)AllItems.HONEYED_APPLE.get())
   );
   BaseRecipeProvider.GeneratedRecipe SWEET_ROLL = this.create(
      "sweet_roll",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Fluids.MILK, 250))
               .require(Items.BREAD))
            .output((ItemLike)AllItems.SWEET_ROLL.get())
   );
   BaseRecipeProvider.GeneratedRecipe CHOCOLATE_BERRIES = this.create(
      "chocolate_glazed_berries",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  (FlowingFluid)AllFluids.CHOCOLATE.get(), 250
               ))
               .require(Items.SWEET_BERRIES))
            .output((ItemLike)AllItems.CHOCOLATE_BERRIES.get())
   );
   BaseRecipeProvider.GeneratedRecipe GRASS_BLOCK = this.create(
      "grass_block",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  net.minecraft.world.level.material.Fluids.WATER, 500
               ))
               .require(Items.DIRT))
            .output(Items.GRASS_BLOCK)
   );
   BaseRecipeProvider.GeneratedRecipe GUNPOWDER = this.create(
      "gunpowder",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  PotionFluidHandler.potionIngredient(Potions.HARMING, 25)
               ))
               .require((ItemLike)AllItems.CINDER_FLOUR.get()))
            .output(Items.GUNPOWDER)
   );
   BaseRecipeProvider.GeneratedRecipe REDSTONE = this.create(
      "redstone",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  PotionFluidHandler.potionIngredient(Potions.STRENGTH, 25)
               ))
               .require((ItemLike)AllItems.CINDER_FLOUR.get()))
            .output(Items.REDSTONE)
   );
   BaseRecipeProvider.GeneratedRecipe GLOWSTONE = this.create(
      "glowstone",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  PotionFluidHandler.potionIngredient(Potions.NIGHT_VISION, 25)
               ))
               .require((ItemLike)AllItems.CINDER_FLOUR.get()))
            .output(Items.GLOWSTONE_DUST)
   );
   BaseRecipeProvider.GeneratedRecipe AM_LAVA = this.create(
      Mods.AM.recipeId("lava_bottle"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     net.minecraft.world.level.material.Fluids.LAVA, 250
                  ))
                  .require(Items.GLASS_BOTTLE))
               .output(1.0F, Mods.AM, "lava_bottle", 1))
            .whenModLoaded(Mods.AM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BWG_LUSH_GRASS = this.create(
      Mods.BWG.recipeId("lush_grass_block"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     Mods.BWG, "lush_dirt"
                  ))
                  .require(net.minecraft.world.level.material.Fluids.WATER, 500))
               .output(Mods.BWG, "lush_grass_block"))
            .whenModLoaded(Mods.BWG.getId())
   );
   BaseRecipeProvider.GeneratedRecipe NEA_MILK = this.create(
      Mods.NEA.recipeId("milk_bottle"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     Fluids.MILK, 250
                  ))
                  .require(Items.GLASS_BOTTLE))
               .output(1.0F, Mods.NEA, "milk_bottle", 1))
            .whenModLoaded(Mods.NEA.getId())
   );
   BaseRecipeProvider.GeneratedRecipe AET_GRASS = this.moddedGrass(Mods.AET, "aether");
   BaseRecipeProvider.GeneratedRecipe RU_PEAT_GRAS = this.moddedGrass(Mods.RU, "peat");
   BaseRecipeProvider.GeneratedRecipe RU_SILT_GRAS = this.moddedGrass(Mods.RU, "silt");
   BaseRecipeProvider.GeneratedRecipe VMP_CURSED_GRASS = this.create(
      Mods.VMP.recipeId("cursed_grass"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     net.minecraft.world.level.material.Fluids.WATER, 500
                  ))
                  .require(Mods.VMP, "cursed_earth"))
               .output(Mods.VMP, "cursed_grass"))
            .whenModLoaded(Mods.VMP.getId())
   );
   BaseRecipeProvider.GeneratedRecipe IE_TREATED_WOOD = this.create(
      Mods.IE.recipeId("treated_wood_in_spout"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     AllTags.AllFluidTags.CREOSOTE.tag, 125
                  ))
                  .require(CreateRecipeProvider.I.planks()))
               .output(Mods.IE, "treated_wood_horizontal"))
            .whenModLoaded(Mods.IE.getId())
   );

   public CreateFillingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
