package dev.simulated_team.simulated.index;

import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllKeys;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.createmod.catnip.client.ConflictSafeKeyMapping;
import net.minecraft.client.KeyMapping;

public enum SimKeys {
   ROTATE_MODE("rotate_mode", 258, "Physics Staff Rotate Mode"),
   SCROLL_UP("scroll_up", InputConstants.UNKNOWN.getValue(), "Scroll Up"),
   SCROLL_DOWN("scroll_down", InputConstants.UNKNOWN.getValue(), "Scroll Down");

   private KeyMapping keybind;
   private final String description;
   private final String translation;
   private final int key;
   private final boolean modifiable;
   private final boolean conflictSafe;

   private SimKeys(final int defaultKey) {
      this("", defaultKey, "");
   }

   private SimKeys(final String description, final int defaultKey, final String translation) {
      this(description, defaultKey, translation, false);
   }

   private SimKeys(final String description, final int defaultKey, final String translation, final boolean conflictSafe) {
      this.description = "simulated.keyinfo." + description;
      this.key = defaultKey;
      this.modifiable = !description.isEmpty();
      this.translation = translation;
      this.conflictSafe = conflictSafe;
   }

   public static void provideLang(BiConsumer<String, String> consumer) {
      for (SimKeys key : values()) {
         if (key.modifiable) {
            consumer.accept(key.description, key.translation);
         }
      }
   }

   public static void registerTo(Consumer<KeyMapping> consumer) {
      for (SimKeys key : values()) {
         if (key.conflictSafe) {
            key.keybind = new ConflictSafeKeyMapping(key.description, key.key, "Create Simulated");
         } else {
            key.keybind = new KeyMapping(key.description, key.key, "Create Simulated");
         }

         if (key.modifiable) {
            consumer.accept(key.keybind);
         }
      }
   }

   public KeyMapping getKeybind() {
      return this.keybind;
   }

   public boolean isPressed() {
      return !this.modifiable ? AllKeys.isKeyDown(this.key) : this.keybind != null && this.keybind.isDown();
   }

   public String getBoundKey() {
      return this.keybind.getTranslatedKeyMessage().getString().toUpperCase();
   }
}
