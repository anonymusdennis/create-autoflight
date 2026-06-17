package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.content.kinetics.millstone.MillingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.tterrag.registrate.util.entry.ItemEntry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;

public abstract class MillingRecipeGen extends StandardProcessingRecipeGen<MillingRecipe> {
   @Deprecated(
      since = "6.0.7",
      forRemoval = true
   )
   @ScheduledForRemoval(
      inVersion = "1.21.1+ Port"
   )
   protected BaseRecipeProvider.GeneratedRecipe metalOre(String name, ItemEntry<? extends Item> crushed, int duration) {
      return this.create(
         name + "_ore",
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        duration
                     ))
                     .withCondition(new NotCondition(new TagEmptyCondition("c", "ores/" + name))))
                  .require(AllTags.commonItemTag("ores/" + name)))
               .output((ItemLike)crushed.get())
      );
   }

   protected BaseRecipeProvider.GeneratedRecipe moddedSandstone(DatagenMod mod, String name) {
      String sandstone = name + "_sandstone";
      return this.create(
         mod.recipeId(sandstone),
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        150
                     ))
                     .require(mod, sandstone))
                  .output(mod, name + "_sand"))
               .whenModLoaded(mod.getId())
      );
   }

   public MillingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.MILLING;
   }
}
