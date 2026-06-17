package net.createmod.ponder.foundation.registration;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.createmod.catnip.data.Couple;
import net.createmod.ponder.api.registration.LangRegistryAccess;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

public class PonderLocalization implements LangRegistryAccess {
   public static final String LANG_PREFIX = "ponder.";
   public static final String UI_PREFIX = "ui.";
   public final Map<ResourceLocation, String> shared = new HashMap<>();
   public final Map<ResourceLocation, Couple<String>> tag = new HashMap<>();
   public final Map<ResourceLocation, Map<String, String>> specific = new HashMap<>();

   public void clearAll() {
      this.shared.clear();
      this.tag.clear();
      this.specific.clear();
   }

   public void clearShared() {
      this.shared.clear();
   }

   public void registerShared(ResourceLocation key, String enUS) {
      this.shared.put(key, enUS);
   }

   public void registerTag(ResourceLocation key, String title, String description) {
      this.tag.put(key, Couple.create(title, description));
   }

   public void registerSpecific(ResourceLocation sceneId, String key, String enUS) {
      this.specific.computeIfAbsent(sceneId, $ -> new HashMap<>()).put(key, enUS);
   }

   protected static String langKeyForShared(ResourceLocation k) {
      return k.getNamespace() + ".ponder.shared." + k.getPath();
   }

   protected static String langKeyForTag(ResourceLocation k) {
      return k.getNamespace() + ".ponder.tag." + k.getPath();
   }

   protected static String langKeyForTagDescription(ResourceLocation k) {
      return k.getNamespace() + ".ponder.tag." + k.getPath() + ".description";
   }

   protected static String langKeyForSpecific(ResourceLocation sceneId, String k) {
      return sceneId.getNamespace() + ".ponder." + sceneId.getPath() + "." + k;
   }

   @Override
   public String getShared(ResourceLocation key) {
      if (PonderIndex.editingModeActive()) {
         return this.shared.containsKey(key) ? this.shared.get(key) : "unregistered shared entry: " + key;
      } else {
         return I18n.get(langKeyForShared(key), new Object[0]);
      }
   }

   @Override
   public String getShared(ResourceLocation key, Object... params) {
      if (PonderIndex.editingModeActive()) {
         return this.shared.containsKey(key) ? String.format(this.shared.get(key), params) : "unregistered shared entry: " + key;
      } else {
         return I18n.get(langKeyForShared(key), params);
      }
   }

   @Override
   public String getTagName(ResourceLocation key) {
      if (PonderIndex.editingModeActive()) {
         return this.tag.containsKey(key) ? this.tag.get(key).getFirst() : "unregistered tag entry: " + key;
      } else {
         return I18n.get(langKeyForTag(key), new Object[0]);
      }
   }

   @Override
   public String getTagDescription(ResourceLocation key) {
      if (PonderIndex.editingModeActive()) {
         return this.tag.containsKey(key) ? this.tag.get(key).getSecond() : "unregistered tag entry: " + key;
      } else {
         return I18n.get(langKeyForTagDescription(key), new Object[0]);
      }
   }

   @Override
   public String getSpecific(ResourceLocation sceneId, String k) {
      if (PonderIndex.editingModeActive()) {
         try {
            return this.specific.get(sceneId).get(k);
         } catch (Exception var4) {
            return "MISSING_SPECIFIC";
         }
      } else {
         return I18n.get(langKeyForSpecific(sceneId, k), new Object[0]);
      }
   }

   @Override
   public String getSpecific(ResourceLocation sceneId, String k, Object... params) {
      if (PonderIndex.editingModeActive()) {
         try {
            return String.format(this.specific.get(sceneId).get(k), params);
         } catch (Exception var5) {
            return "MISSING_SPECIFIC";
         }
      } else {
         return I18n.get(langKeyForSpecific(sceneId, k), params);
      }
   }

   private void recordGeneral(BiConsumer<String, String> consumer) {
      this.addGeneral(consumer, "ui.hold_to_ponder", "Hold [%1$s] to Ponder");
      this.addGeneral(consumer, "ui.subject", "Subject of this scene");
      this.addGeneral(consumer, "ui.pondering", "Pondering about...");
      this.addGeneral(consumer, "ui.pondering_tag", "Pondering about...");
      this.addGeneral(consumer, "ui.identify_mode", "Identify mode active.\nUnpause with [%1$s]");
      this.addGeneral(consumer, "ui.associated", "Associated Entries");
      this.addGeneral(consumer, "ui.close", "Close");
      this.addGeneral(consumer, "ui.identify", "Identify");
      this.addGeneral(consumer, "ui.next", "Next Scene");
      this.addGeneral(consumer, "ui.next_up", "Up Next:");
      this.addGeneral(consumer, "ui.previous", "Previous Scene");
      this.addGeneral(consumer, "ui.replay", "Replay");
      this.addGeneral(consumer, "ui.think_back", "Think Back");
      this.addGeneral(consumer, "ui.slow_text", "Comfy Reading");
      this.addGeneral(consumer, "ui.exit", "Exit");
      this.addGeneral(consumer, "ui.welcome", "Welcome to Ponder");
      this.addGeneral(consumer, "ui.categories", "Available Categories for %1$s");
      this.addGeneral(consumer, "ui.index_description", "Click one of the icons below to learn about its associated Items and Blocks");
      this.addGeneral(consumer, "ui.index_title", "Ponder Index");
   }

   private void addGeneral(BiConsumer<String, String> consumer, String key, String enUS) {
      consumer.accept("ponder." + key, enUS);
   }

   public void generateSceneLang() {
      PonderIndex.getSceneAccess().getRegisteredEntries().forEach(entry -> PonderSceneRegistry.compileScene(this, entry.getValue(), null));
   }

   @Override
   public void provideLang(String modId, BiConsumer<String, String> consumer) {
      PonderIndex.registerAll();
      PonderIndex.gatherSharedText();
      this.generateSceneLang();
      if (modId.equals("ponder")) {
         this.recordGeneral(consumer);
      }

      this.shared.forEach((k, v) -> {
         if (k.getNamespace().equals(modId)) {
            consumer.accept(langKeyForShared(k), v);
         }
      });
      this.tag.forEach((k, v) -> {
         if (k.getNamespace().equals(modId)) {
            consumer.accept(langKeyForTag(k), v.getFirst());
            consumer.accept(langKeyForTagDescription(k), v.getSecond());
         }
      });
      this.specific
         .entrySet()
         .stream()
         .filter(entry -> entry.getKey().getNamespace().equals(modId))
         .sorted(Entry.comparingByKey())
         .forEach(
            entry -> entry.getValue()
                  .entrySet()
                  .stream()
                  .sorted(Entry.comparingByKey())
                  .forEach(subEntry -> consumer.accept(langKeyForSpecific(entry.getKey(), subEntry.getKey()), subEntry.getValue()))
         );
   }
}
