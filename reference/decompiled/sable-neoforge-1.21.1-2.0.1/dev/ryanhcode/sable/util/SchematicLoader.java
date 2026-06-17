package dev.ryanhcode.sable.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

public class SchematicLoader {
   @Nullable
   public static StructureTemplate loadSchematic(ServerLevel level, ResourceLocation location) {
      String namespace = location.getNamespace();
      String path = "schematics/" + location.getPath() + ".nbt";
      ResourceLocation location1 = ResourceLocation.fromNamespaceAndPath(namespace, path);
      Optional<Resource> option = level.getServer().getResourceManager().getResource(location1);
      if (option.isEmpty()) {
         return null;
      } else {
         Resource resource = option.get();

         try {
            StructureTemplate var10;
            try (InputStream stream = resource.open()) {
               StructureTemplate template = new StructureTemplate();
               CompoundTag nbt = NbtIo.readCompressed(stream, NbtAccounter.create(536870912L));
               template.load(level.holderLookup(Registries.BLOCK), nbt);
               var10 = template;
            }

            return var10;
         } catch (IOException var13) {
            return null;
         }
      }
   }

   public static CompletableFuture<Set<ResourceLocation>> getSchematics(MinecraftServer server) {
      return CompletableFuture.supplyAsync(
         () -> server.getResourceManager().listResources("schematics", path -> path.getPath().endsWith(".nbt")).keySet(), Util.backgroundExecutor()
      );
   }
}
