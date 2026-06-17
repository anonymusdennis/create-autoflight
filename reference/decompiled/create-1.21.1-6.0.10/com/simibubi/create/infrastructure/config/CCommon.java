package com.simibubi.create.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;

public class CCommon extends ConfigBase {
   public final CWorldGen worldGen = (CWorldGen)this.nested(0, CWorldGen::new, new String[]{CCommon.Comments.worldGen});

   public String getName() {
      return "common";
   }

   private static class Comments {
      static String worldGen = "Modify Create's impact on your terrain";
   }
}
