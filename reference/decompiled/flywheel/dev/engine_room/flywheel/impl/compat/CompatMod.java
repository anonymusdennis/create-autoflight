package dev.engine_room.flywheel.impl.compat;

import dev.engine_room.flywheel.impl.FlwImplXplat;

public enum CompatMod {
   EMBEDDIUM("embeddium"),
   IRIS("iris"),
   SODIUM("sodium");

   public final String id;
   public final boolean isLoaded;

   private CompatMod(String modId) {
      this.id = modId;
      this.isLoaded = FlwImplXplat.INSTANCE.isModLoaded(modId);
   }
}
