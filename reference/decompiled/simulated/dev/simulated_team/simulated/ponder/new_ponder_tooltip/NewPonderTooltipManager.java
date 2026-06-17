package dev.simulated_team.simulated.ponder.new_ponder_tooltip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.simulated_team.simulated.Simulated;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class NewPonderTooltipManager {
   private static final Codec<Set<ResourceLocation>> CODEC = ResourceLocation.CODEC.listOf().xmap(HashSet::new, set -> set.stream().toList());
   private static final HashMap<Item, Set<ResourceLocation>> NEW_PONDER_SCENES = new HashMap<>();
   private static Set<ResourceLocation> WATCHED_PONDER_SCENES = null;

   private static Path filePath() {
      return Minecraft.getInstance().gameDirectory.toPath().resolve("ponders_watched.json");
   }

   public static NewPonderTooltipManager.RegisterBuilder forItems(Item... item) {
      return new NewPonderTooltipManager.RegisterBuilder(item);
   }

   public static boolean hasWatchedAllScenes(Item item) {
      load();
      if (NEW_PONDER_SCENES.containsKey(item)) {
         Set<ResourceLocation> scenes = NEW_PONDER_SCENES.get(item);
         return WATCHED_PONDER_SCENES.containsAll(scenes);
      } else {
         return true;
      }
   }

   public static void setSceneWatched(ResourceLocation id) {
      load();
      if (WATCHED_PONDER_SCENES != null && !hasWatchedScene(id)) {
         WATCHED_PONDER_SCENES.add(id);
         save();
      }
   }

   public static boolean hasWatchedScene(ResourceLocation id) {
      load();
      return WATCHED_PONDER_SCENES.contains(id);
   }

   public static void save() {
      DataResult<JsonElement> result = CODEC.encode(WATCHED_PONDER_SCENES, JsonOps.INSTANCE, new JsonArray());
      if (!result.isError()) {
         try {
            String data = ((JsonElement)result.getOrThrow()).toString();
            Files.writeString(filePath(), data, StandardCharsets.UTF_8);
         } catch (IOException var2) {
         }
      }
   }

   public static void load() {
      if (WATCHED_PONDER_SCENES == null) {
         DataResult<Set<ResourceLocation>> result = CODEC.parse(JsonOps.INSTANCE, getOrCreateFile());
         WATCHED_PONDER_SCENES = new HashSet<>();
         result.ifSuccess(set -> WATCHED_PONDER_SCENES.addAll(set));
      }
   }

   @NotNull
   private static JsonElement getOrCreateFile() {
      Path path = filePath();
      String jsonString = "[]";

      try {
         File file = path.toFile();
         if (file.exists()) {
            jsonString = Files.readString(path);
         } else {
            Files.writeString(path, jsonString);
         }
      } catch (IOException var5) {
         Simulated.LOGGER.info("There was an error reading ponders_watched.json.");
      }

      JsonElement element = new JsonArray();

      try {
         element = JsonParser.parseString(jsonString);
      } catch (JsonSyntaxException var4) {
         Simulated.LOGGER.info("ponders_watched.json was malformed.");
      }

      return element;
   }

   public static record RegisterBuilder(Item... items) {
      public NewPonderTooltipManager.RegisterBuilder addScenes(ResourceLocation... scenes) {
         Set<ResourceLocation> sceneSet = new HashSet<>(List.of(scenes));

         for (Item item : this.items) {
            NewPonderTooltipManager.NEW_PONDER_SCENES.computeIfAbsent(item, k -> new HashSet<>()).addAll(sceneSet);
         }

         return this;
      }
   }
}
