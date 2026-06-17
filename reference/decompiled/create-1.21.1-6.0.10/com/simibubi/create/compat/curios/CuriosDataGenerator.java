package com.simibubi.create.compat.curios;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import top.theillusivec4.curios.api.CuriosDataProvider;

public class CuriosDataGenerator extends CuriosDataProvider {
   public CuriosDataGenerator(PackOutput output, CompletableFuture<Provider> registries, ExistingFileHelper fileHelper) {
      super("create", output, fileHelper, registries);
   }

   public void generate(Provider registries, ExistingFileHelper fileHelper) {
      this.createEntities("players").addPlayer().addSlots(new String[]{"head"});
   }
}
