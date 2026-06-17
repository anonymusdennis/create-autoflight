package dev.simulated_team.simulated.neoforge.compat.jei;

import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimBlocks;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.core.NonNullList;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.Ingredient.ItemValue;
import net.minecraft.world.item.crafting.Ingredient.TagValue;
import net.minecraft.world.item.crafting.Ingredient.Value;
import net.minecraft.world.level.block.Block;

public final class PortableEngineDyeingRecipeMaker {
   public static Stream<RecipeHolder<CraftingRecipe>> createRecipes() {
      String group = "simulated.portable_engine.color";
      ItemStack base = SimBlocks.PORTABLE_ENGINES.get(DyeColor.RED).asStack();
      Ingredient baseIngredient = Ingredient.of(new ItemStack[]{base});
      return Arrays.stream(DyeColor.values()).filter(dc -> dc != DyeColor.RED).map(color -> {
         DyeItem dye = DyeItem.byColor(color);
         ItemStack dyeStack = new ItemStack(dye);
         TagKey<Item> colorTag = color.getTag();
         Value dyeList = new ItemValue(dyeStack);
         Value colorList = new TagValue(colorTag);
         Stream<Value> colorIngredientStream = Stream.of(dyeList, colorList);
         Ingredient colorIngredient = Ingredient.fromValues(colorIngredientStream);
         NonNullList<Ingredient> inputs = NonNullList.of(Ingredient.EMPTY, new Ingredient[]{baseIngredient, colorIngredient});
         Block coloredShulkerBox = (Block)SimBlocks.PORTABLE_ENGINES.get(color).get();
         ItemStack output = new ItemStack(coloredShulkerBox);
         ShapelessRecipe recipe = new ShapelessRecipe("simulated.portable_engine.color", CraftingBookCategory.MISC, output, inputs);
         return new RecipeHolder(Simulated.path("simulated.portable_engine.color/" + color), recipe);
      });
   }

   private PortableEngineDyeingRecipeMaker() {
   }
}
