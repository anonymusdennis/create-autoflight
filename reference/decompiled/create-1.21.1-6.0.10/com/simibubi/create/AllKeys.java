package com.simibubi.create;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.BiConsumer;
import net.createmod.catnip.client.ConflictSafeKeyMapping;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber({Dist.CLIENT})
public enum AllKeys {
   TOOL_MENU("toolmenu", 342, "Focus Schematic Overlay"),
   ACTIVATE_TOOL(341),
   TOOLBELT("toolbelt", 342, "Access Nearby Toolboxes"),
   ROTATE_MENU("rotate_menu", -1, "Open Block Rotation Menu"),
   SHIFT_MODIFIER("shift_modifier", 340, "Shift Modifier", true),
   CTRL_MODIFIER("ctrl_modifier", 341, "Ctrl Modifier", true),
   ALT_MODIFIER("alt_modifier", 342, "Alt Modifier", true);

   private KeyMapping keybind;
   private final String description;
   private final String translation;
   private final int key;
   private final boolean modifiable;
   private final boolean conflictSafe;

   private AllKeys(int defaultKey) {
      this("", defaultKey, "");
   }

   private AllKeys(String description, int defaultKey, String translation) {
      this(description, defaultKey, translation, false);
   }

   private AllKeys(String description, int defaultKey, String translation, boolean conflictSafe) {
      this.description = "create.keyinfo." + description;
      this.key = defaultKey;
      this.modifiable = !description.isEmpty();
      this.translation = translation;
      this.conflictSafe = conflictSafe;
   }

   public static void provideLang(BiConsumer<String, String> consumer) {
      for (AllKeys key : values()) {
         if (key.modifiable) {
            consumer.accept(key.description, key.translation);
         }
      }
   }

   @SubscribeEvent
   public static void register(RegisterKeyMappingsEvent event) {
      for (AllKeys key : values()) {
         if (key.conflictSafe) {
            key.keybind = new ConflictSafeKeyMapping(key.description, key.key, "Create");
         } else {
            key.keybind = new KeyMapping(key.description, key.key, "Create");
         }

         if (key.modifiable) {
            event.register(key.keybind);
         }
      }
   }

   public KeyMapping getKeybind() {
      return this.keybind;
   }

   public boolean isPressed() {
      return !this.modifiable ? isKeyDown(this.key) : this.keybind.isDown();
   }

   public String getBoundKey() {
      return this.keybind.getTranslatedKeyMessage().getString().toUpperCase();
   }

   public boolean doesModifierAndCodeMatch(int code) {
      boolean codeMatches = code == this.keybind.getKey().getValue();
      KeyModifier modifier = this.keybind.getKeyModifier();
      boolean modifierMatches;
      if (modifier == KeyModifier.NONE) {
         modifierMatches = true;
      } else {
         modifierMatches = KeyModifier.getActiveModifiers().contains(modifier);
      }

      return codeMatches && modifierMatches;
   }

   public static boolean isKeyDown(int key) {
      return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
   }

   public static boolean isMouseButtonDown(int button) {
      return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), button) == 1;
   }

   public static boolean ctrlDown() {
      return isKeyDown(CTRL_MODIFIER.keybind.getKey().getValue());
   }

   public static boolean shiftDown() {
      return isKeyDown(SHIFT_MODIFIER.keybind.getKey().getValue());
   }

   public static boolean altDown() {
      return isKeyDown(ALT_MODIFIER.keybind.getKey().getValue());
   }
}
