package com.simibubi.create.foundation.pack;

import com.google.gson.JsonElement;
import com.simibubi.create.Create;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources.ResourceOutput;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DynamicPack implements PackResources {
   private final Map<String, IoSupplier<InputStream>> files = new HashMap<>();
   private final String packId;
   private final PackType packType;
   private final PackMetadataSection metadata;
   private final PackLocationInfo packLocationInfo;

   public DynamicPack(String packId, PackType packType) {
      this.packId = packId;
      this.packType = packType;
      this.metadata = new PackMetadataSection(Component.empty(), SharedConstants.getCurrentVersion().getPackVersion(packType));
      this.packLocationInfo = new PackLocationInfo(packId, Component.literal(packId), PackSource.BUILT_IN, Optional.empty());
   }

   private static String getPath(PackType packType, ResourceLocation resourceLocation) {
      return packType.getDirectory() + "/" + resourceLocation.getNamespace() + "/" + resourceLocation.getPath();
   }

   public DynamicPack put(ResourceLocation location, IoSupplier<InputStream> stream) {
      this.files.put(getPath(this.packType, location), stream);
      return this;
   }

   public DynamicPack put(ResourceLocation location, byte[] bytes) {
      return this.put(location, (IoSupplier<InputStream>)(() -> new ByteArrayInputStream(bytes)));
   }

   public DynamicPack put(ResourceLocation location, String string) {
      return this.put(location, string.getBytes(StandardCharsets.UTF_8));
   }

   public DynamicPack put(ResourceLocation location, JsonElement json) {
      return this.put(location.withSuffix(".json"), Create.GSON.toJson(json));
   }

   @Nullable
   public IoSupplier<InputStream> getRootResource(@NotNull String... elements) {
      return this.files.getOrDefault(String.join("/", elements), null);
   }

   @Nullable
   public IoSupplier<InputStream> getResource(@NotNull PackType packType, @NotNull ResourceLocation resourceLocation) {
      return this.files.getOrDefault(getPath(packType, resourceLocation), null);
   }

   public void listResources(@NotNull PackType packType, @NotNull String namespace, @NotNull String path, @NotNull ResourceOutput resourceOutput) {
      ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(namespace, path);
      String directoryAndNamespace = packType.getDirectory() + "/" + namespace + "/";
      String prefix = directoryAndNamespace + path + "/";
      this.files.forEach((filePath, streamSupplier) -> {
         if (filePath.startsWith(prefix)) {
            resourceOutput.accept(resourceLocation.withPath(filePath.substring(directoryAndNamespace.length())), streamSupplier);
         }
      });
   }

   @NotNull
   public Set<String> getNamespaces(PackType packType) {
      Set<String> namespaces = new HashSet<>();
      String dir = packType.getDirectory() + "/";

      for (String path : this.files.keySet()) {
         if (path.startsWith(dir)) {
            String relative = path.substring(dir.length());
            if (relative.contains("/")) {
               namespaces.add(relative.substring(0, relative.indexOf("/")));
            }
         }
      }

      return namespaces;
   }

   @Nullable
   public <T> T getMetadataSection(@NotNull MetadataSectionSerializer<T> deserializer) throws IOException {
      return (T)(deserializer == PackMetadataSection.TYPE ? this.metadata : null);
   }

   @NotNull
   public PackLocationInfo location() {
      return this.packLocationInfo;
   }

   @NotNull
   public String packId() {
      return this.packId;
   }

   public void close() {
   }
}
