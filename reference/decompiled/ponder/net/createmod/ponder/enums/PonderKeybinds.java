package net.createmod.ponder.enums;

import java.util.function.Consumer;
import net.createmod.catnip.client.ConflictSafeKeyMapping;
import net.createmod.catnip.platform.CatnipClientServices;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public enum PonderKeybinds {
   PONDER("ponder", 87);

   public static final String CATEGORY = "key.categories.ponder";
   private final KeyMapping mapping;

   private PonderKeybinds(String description, int defaultKey) {
      this.mapping = new ConflictSafeKeyMapping("key.ponder." + description, defaultKey, "key.categories.ponder");
   }

   public static void register(Consumer<KeyMapping> registrationCallback) {
      for (PonderKeybinds key : values()) {
         registrationCallback.accept(key.mapping);
      }
   }

   public boolean isDown() {
      return !this.mapping.isUnbound() && CatnipClientServices.CLIENT_HOOKS.isKeyPressed(this.mapping);
   }

   public Component message() {
      return this.mapping.getTranslatedKeyMessage();
   }
}
