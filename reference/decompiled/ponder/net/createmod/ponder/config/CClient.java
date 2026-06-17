package net.createmod.ponder.config;

import net.createmod.catnip.config.ConfigBase;

public class CClient extends ConfigBase {
   public final ConfigBase.ConfigBool comfyReading = this.b(false, "comfyReading", new String[]{CClient.Comments.comfyReading});
   public final ConfigBase.ConfigBool editingMode = this.b(false, "editingMode", new String[]{CClient.Comments.editingMode});
   public final ConfigBase.ConfigGroup placementAssist = this.group(1, "placementAssist", new String[]{CClient.Comments.placementAssist});
   public final ConfigBase.ConfigEnum<CClient.PlacementIndicatorSetting> placementIndicator = this.e(
      CClient.PlacementIndicatorSetting.TEXTURE, "indicatorType", CClient.Comments.placementIndicator
   );
   public final ConfigBase.ConfigFloat indicatorScale = this.f(1.0F, 0.0F, "indicatorScale", new String[]{CClient.Comments.indicatorScale});

   @Override
   public String getName() {
      return "client";
   }

   private static class Comments {
      static String comfyReading = "Slow down a ponder scene whenever there is text on screen.";
      static String editingMode = "Show additional info in the ponder view and reload scene scripts more frequently.";
      static String placementAssist = "Settings for the Placement Assist";
      static String[] placementIndicator = new String[]{
         "What indicator should be used when showing where the assisted placement ends up relative to your crosshair",
         "Choose 'NONE' to disable the Indicator altogether"
      };
      static String indicatorScale = "Change the size of the Indicator by this multiplier";
   }

   public static enum PlacementIndicatorSetting {
      TEXTURE,
      TRIANGLE,
      NONE;
   }
}
