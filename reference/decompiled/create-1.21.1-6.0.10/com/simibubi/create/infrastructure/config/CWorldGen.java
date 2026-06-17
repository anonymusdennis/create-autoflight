package com.simibubi.create.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigBool;

public class CWorldGen extends ConfigBase {
   public final ConfigBool disable = this.b(false, "disableWorldGen", new String[]{CWorldGen.Comments.disable});

   public String getName() {
      return "worldgen";
   }

   private static class Comments {
      static String disable = "Prevents all worldgen added by Create from taking effect";
   }
}
