package net.createmod.ponder.foundation.registration;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nullable;
import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.SceneRegistryAccess;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.foundation.PonderIndex;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class PonderSceneRegistry implements SceneRegistryAccess {
   private final PonderLocalization localization;
   private final Multimap<ResourceLocation, StoryBoardEntry> scenes;
   private boolean allowRegistration = true;

   public PonderSceneRegistry(PonderLocalization localization) {
      this.localization = localization;
      this.scenes = LinkedHashMultimap.create();
   }

   public void clearRegistry() {
      this.scenes.clear();
      this.allowRegistration = true;
   }

   public void addStoryBoard(StoryBoardEntry entry) {
      if (!this.allowRegistration) {
         throw new IllegalStateException("Registration Phase has already ended!");
      } else {
         this.scenes.put(entry.getComponent(), entry);
      }
   }

   @Override
   public Collection<Entry<ResourceLocation, StoryBoardEntry>> getRegisteredEntries() {
      return this.scenes.entries();
   }

   @Override
   public boolean doScenesExistForId(ResourceLocation id) {
      return this.scenes.containsKey(id);
   }

   @Override
   public List<PonderScene> compile(ResourceLocation id) {
      if (PonderIndex.editingModeActive()) {
         PonderIndex.reload();
      }

      Collection<StoryBoardEntry> entries = this.scenes.get(id);
      return entries.isEmpty() ? Collections.emptyList() : this.compile(entries);
   }

   @Override
   public List<PonderScene> compile(Collection<StoryBoardEntry> entries) {
      if (PonderIndex.editingModeActive()) {
         this.localization.clearShared();
         PonderIndex.gatherSharedText();
      }

      List<PonderScene> scenes = new ArrayList<>();

      for (StoryBoardEntry storyBoard : entries) {
         StructureTemplate activeTemplate = loadSchematic(storyBoard.getSchematicLocation());
         PonderLevel level = new PonderLevel(BlockPos.ZERO, Minecraft.getInstance().level);
         activeTemplate.placeInWorld(level, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(), level.random, 2);
         level.createBackup();
         PonderScene scene = compileScene(this.localization, storyBoard, level);
         scene.begin();
         scenes.add(scene);
      }

      return scenes;
   }

   public static PonderScene compileScene(PonderLocalization localization, StoryBoardEntry sb, @Nullable PonderLevel level) {
      PonderScene scene = new PonderScene(level, localization, sb.getNamespace(), sb.getComponent(), sb.getTags(), sb.getOrderingEntries());
      SceneBuilder builder = scene.builder();
      sb.getBoard().program(builder, scene.getSceneBuildingUtil());
      return scene;
   }

   public static StructureTemplate loadSchematic(ResourceLocation location) {
      return loadSchematic(Minecraft.getInstance().getResourceManager(), location);
   }

   public static StructureTemplate loadSchematic(ResourceManager resourceManager, ResourceLocation location) {
      String namespace = location.getNamespace();
      String path = "ponder/" + location.getPath() + ".nbt";
      ResourceLocation location1 = ResourceLocation.fromNamespaceAndPath(namespace, path);
      Optional<Resource> optionalResource = resourceManager.getResource(location1);
      if (optionalResource.isEmpty()) {
         Ponder.LOGGER.error("Ponder schematic missing: " + location1);
         return new StructureTemplate();
      } else {
         Resource resource = optionalResource.get();

         try {
            StructureTemplate var8;
            try (InputStream inputStream = resource.open()) {
               var8 = loadSchematic(inputStream);
            }

            return var8;
         } catch (IOException var12) {
            Ponder.LOGGER.error("Failed to read ponder schematic: " + location1, var12);
            return new StructureTemplate();
         }
      }
   }

   public static StructureTemplate loadSchematic(InputStream resourceStream) throws IOException {
      StructureTemplate t = new StructureTemplate();
      DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(resourceStream)));
      CompoundTag nbt = NbtIo.read(stream, NbtAccounter.create(536870912L));
      t.load(BuiltInRegistries.BLOCK.asLookup(), nbt);
      return t;
   }
}
