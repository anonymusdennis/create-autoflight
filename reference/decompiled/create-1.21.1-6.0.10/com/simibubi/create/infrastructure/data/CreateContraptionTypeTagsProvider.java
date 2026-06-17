package com.simibubi.create.infrastructure.data;

import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.api.registry.CreateRegistries;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class CreateContraptionTypeTagsProvider extends TagsProvider<ContraptionType> {
   public CreateContraptionTypeTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
      super(output, CreateRegistries.CONTRAPTION_TYPE, lookupProvider, "create", existingFileHelper);
   }

   protected void addTags(Provider pProvider) {
      this.tag(AllTags.AllContraptionTypeTags.OPENS_CONTROLS.tag).add(AllContraptionTypes.CARRIAGE.key());
      this.tag(AllTags.AllContraptionTypeTags.REQUIRES_VEHICLE_FOR_RENDER.tag).add(AllContraptionTypes.MOUNTED.key());
   }

   public String getName() {
      return "Create's Contraption Type Tags";
   }
}
