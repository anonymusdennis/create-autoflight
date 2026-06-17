package net.createmod.catnip.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;

public class ConflictSafeKeyMapping extends KeyMapping {
   public ConflictSafeKeyMapping(String description, int defaultKey, String category) {
      super(description, defaultKey, category);
   }

   public ConflictSafeKeyMapping(String description, Type type, int defaultKey, String category) {
      super(description, type, defaultKey, category);
   }
}
