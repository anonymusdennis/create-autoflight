package com.simibubi.create.infrastructure.data;

import com.simibubi.create.AllEnchantments;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

public class CreateEnchantmentTagsProvider extends EnchantmentTagsProvider {
   public CreateEnchantmentTagsProvider(PackOutput output, CompletableFuture<Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
      super(output, lookupProvider, "create", existingFileHelper);
   }

   protected void addTags(Provider prov) {
      this.tag(EnchantmentTags.NON_TREASURE).add(new ResourceKey[]{AllEnchantments.CAPACITY, AllEnchantments.POTATO_RECOVERY});
      this.tag(EnchantmentTags.IN_ENCHANTING_TABLE).add(new ResourceKey[]{AllEnchantments.CAPACITY, AllEnchantments.POTATO_RECOVERY});
   }
}
