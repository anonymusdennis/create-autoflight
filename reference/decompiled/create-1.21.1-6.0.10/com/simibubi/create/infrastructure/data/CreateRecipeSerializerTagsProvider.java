package com.simibubi.create.infrastructure.data;

import com.simibubi.create.AllTags;
import com.simibubi.create.compat.Mods;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class CreateRecipeSerializerTagsProvider extends TagsProvider<RecipeSerializer<?>> {
   public CreateRecipeSerializerTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
      super(output, Registries.RECIPE_SERIALIZER, lookupProvider, "create", existingFileHelper);
   }

   protected void addTags(Provider pProvider) {
      this.tag(AllTags.AllRecipeSerializerTags.AUTOMATION_IGNORE.tag).addOptional(Mods.OCCULTISM.rl("spirit_trade")).addOptional(Mods.OCCULTISM.rl("ritual"));
   }

   public String getName() {
      return "Create's Recipe Serializer Tags";
   }
}
