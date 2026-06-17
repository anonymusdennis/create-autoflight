package com.simibubi.create.foundation.pack;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.Pack.Metadata;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.Pack.ResourcesSupplier;
import org.jetbrains.annotations.NotNull;

public record DynamicPackSource(String packId, PackType packType, Position packPosition, PackResources packResources) implements RepositorySource {
   public void loadPacks(@NotNull Consumer<Pack> onLoad) {
      PackLocationInfo locationInfo = new PackLocationInfo(this.packId, Component.literal(this.packId), PackSource.BUILT_IN, Optional.empty());
      PackSelectionConfig selectionConfig = new PackSelectionConfig(true, this.packPosition, true);
      ResourcesSupplier resourcesSupplier = new ResourcesSupplier() {
         @NotNull
         public PackResources openPrimary(@NotNull PackLocationInfo packLocationInfo) {
            return DynamicPackSource.this.packResources;
         }

         @NotNull
         public PackResources openFull(@NotNull PackLocationInfo packLocationInfo, @NotNull Metadata metadata) {
            return DynamicPackSource.this.packResources;
         }
      };
      onLoad.accept(Pack.readMetaAndCreate(locationInfo, resourcesSupplier, this.packType, selectionConfig));
   }
}
