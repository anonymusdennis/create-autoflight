package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.HauntingRecipeGen;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public final class CreateHauntingRecipeGen extends HauntingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe BRASS_BELL = this.convert(
      () -> Ingredient.of(new ItemLike[]{(ItemLike)AllBlocks.PECULIAR_BELL.get()}), AllBlocks.HAUNTED_BELL::get
   );
   BaseRecipeProvider.GeneratedRecipe HAUNT_STONE = this.convert(Items.STONE, Items.INFESTED_STONE);
   BaseRecipeProvider.GeneratedRecipe HAUNT_DEEPSLATE = this.convert(Items.DEEPSLATE, Items.INFESTED_DEEPSLATE);
   BaseRecipeProvider.GeneratedRecipe HAUNT_STONE_BRICKS = this.convert(Items.STONE_BRICKS, Items.INFESTED_STONE_BRICKS);
   BaseRecipeProvider.GeneratedRecipe HAUNT_MOSSY_STONE_BRICKS = this.convert(Items.MOSSY_STONE_BRICKS, Items.INFESTED_MOSSY_STONE_BRICKS);
   BaseRecipeProvider.GeneratedRecipe HAUNT_CRACKED_STONE_BRICKS = this.convert(Items.CRACKED_STONE_BRICKS, Items.INFESTED_CRACKED_STONE_BRICKS);
   BaseRecipeProvider.GeneratedRecipe HAUNT_CHISELED_STONE_BRICKS = this.convert(Items.CHISELED_STONE_BRICKS, Items.INFESTED_CHISELED_STONE_BRICKS);
   BaseRecipeProvider.GeneratedRecipe SOUL_TORCH = this.convert(Items.TORCH, Items.SOUL_TORCH);
   BaseRecipeProvider.GeneratedRecipe SOUL_CAMPFIRE = this.convert(Items.CAMPFIRE, Items.SOUL_CAMPFIRE);
   BaseRecipeProvider.GeneratedRecipe SOUL_LANTERN = this.convert(Items.LANTERN, Items.SOUL_LANTERN);
   BaseRecipeProvider.GeneratedRecipe POISON_POTATO = this.convert(Items.POTATO, Items.POISONOUS_POTATO);
   BaseRecipeProvider.GeneratedRecipe GLOW_INK = this.convert(Items.INK_SAC, Items.GLOW_INK_SAC);
   BaseRecipeProvider.GeneratedRecipe GLOW_BERRIES = this.convert(Items.SWEET_BERRIES, Items.GLOW_BERRIES);
   BaseRecipeProvider.GeneratedRecipe NETHER_BRICK = this.convert(Items.BRICK, Items.NETHER_BRICK);
   BaseRecipeProvider.GeneratedRecipe PRISMARINE = this.create(
      Create.asResource("lapis_recycling"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  net.neoforged.neoforge.common.Tags.Items.GEMS_LAPIS
               ))
               .output(0.75F, Items.PRISMARINE_SHARD))
            .output(0.125F, Items.PRISMARINE_CRYSTALS)
   );
   BaseRecipeProvider.GeneratedRecipe SOUL_SAND = this.convert(() -> Ingredient.of(ItemTags.SAND), () -> Blocks.SOUL_SAND);
   BaseRecipeProvider.GeneratedRecipe SOUL_DIRT = this.convert(() -> Ingredient.of(ItemTags.DIRT), () -> Blocks.SOUL_SOIL);
   BaseRecipeProvider.GeneratedRecipe BLACK_STONE = this.convert(
      () -> Ingredient.of(net.neoforged.neoforge.common.Tags.Items.COBBLESTONES), () -> Blocks.BLACKSTONE
   );
   BaseRecipeProvider.GeneratedRecipe CRIMSON_FUNGUS = this.convert(Items.RED_MUSHROOM, Items.CRIMSON_FUNGUS);
   BaseRecipeProvider.GeneratedRecipe WARPED_FUNGUS = this.convert(Items.BROWN_MUSHROOM, Items.WARPED_FUNGUS);
   BaseRecipeProvider.GeneratedRecipe FD = this.moddedConversion(Mods.FD, "tomato", "rotten_tomato");
   BaseRecipeProvider.GeneratedRecipe HH = this.create(
      Mods.HH.recipeId("rotten_apple"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Items.APPLE))
               .output(Mods.HH, "rotten_apple"))
            .whenModLoaded(Mods.HH.getId())
   );

   public CreateHauntingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
