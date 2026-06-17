package com.simibubi.create.content.kinetics.crafter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.foundation.mixin.accessor.ShapedRecipeAccessor;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class MechanicalCraftingRecipe extends ShapedRecipe {
   private final boolean acceptMirrored;

   public MechanicalCraftingRecipe(String groupIn, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack recipeOutputIn, boolean acceptMirrored) {
      super(groupIn, category, pattern, recipeOutputIn, acceptMirrored);
      this.acceptMirrored = acceptMirrored;
   }

   private static MechanicalCraftingRecipe fromShaped(ShapedRecipe recipe, boolean acceptMirrored) {
      return new MechanicalCraftingRecipe(
         recipe.getGroup(), recipe.category(), ((ShapedRecipeAccessor)recipe).create$getPattern(), recipe.getResultItem(null), acceptMirrored
      );
   }

   public boolean matches(CraftingInput input, Level worldIn) {
      if (!(input instanceof MechanicalCraftingInput)) {
         return false;
      } else if (this.acceptsMirrored()) {
         return super.matches(input, worldIn);
      } else {
         for (int i = 0; i <= input.width() - this.getWidth(); i++) {
            for (int j = 0; j <= input.height() - this.getHeight(); j++) {
               if (this.matchesSpecific(input, i, j)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean matchesSpecific(CraftingInput input, int p_77573_2_, int p_77573_3_) {
      NonNullList<Ingredient> ingredients = this.getIngredients();
      int width = this.getWidth();
      int height = this.getHeight();

      for (int i = 0; i < input.width(); i++) {
         for (int j = 0; j < input.height(); j++) {
            int k = i - p_77573_2_;
            int l = j - p_77573_3_;
            Ingredient ingredient = Ingredient.EMPTY;
            if (k >= 0 && l >= 0 && k < width && l < height) {
               ingredient = (Ingredient)ingredients.get(k + l * width);
            }

            if (!ingredient.test(input.getItem(i + j * input.width()))) {
               return false;
            }
         }
      }

      return true;
   }

   public RecipeType<?> getType() {
      return AllRecipeTypes.MECHANICAL_CRAFTING.getType();
   }

   public boolean isSpecial() {
      return true;
   }

   @NotNull
   public RecipeSerializer<?> getSerializer() {
      return AllRecipeTypes.MECHANICAL_CRAFTING.getSerializer();
   }

   public boolean acceptsMirrored() {
      return this.acceptMirrored;
   }

   public static class Serializer implements RecipeSerializer<MechanicalCraftingRecipe> {
      public static final MapCodec<MechanicalCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
                  RecipeSerializer.SHAPED_RECIPE.codec().forGetter(t -> t),
                  Codec.BOOL.fieldOf("accept_mirrored").forGetter(MechanicalCraftingRecipe::acceptsMirrored)
               )
               .apply(instance, MechanicalCraftingRecipe::fromShaped)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, MechanicalCraftingRecipe> STREAM_CODEC = StreamCodec.composite(
         net.minecraft.world.item.crafting.ShapedRecipe.Serializer.STREAM_CODEC,
         i -> i,
         ByteBufCodecs.BOOL,
         i -> i.acceptMirrored,
         MechanicalCraftingRecipe::fromShaped
      );

      @NotNull
      public MapCodec<MechanicalCraftingRecipe> codec() {
         return CODEC;
      }

      @NotNull
      public StreamCodec<RegistryFriendlyByteBuf, MechanicalCraftingRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
