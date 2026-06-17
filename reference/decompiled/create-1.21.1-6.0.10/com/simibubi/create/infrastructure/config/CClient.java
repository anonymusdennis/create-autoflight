package com.simibubi.create.infrastructure.config;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.createmod.catnip.config.ConfigBase.ConfigEnum;
import net.createmod.catnip.config.ConfigBase.ConfigFloat;
import net.createmod.catnip.config.ConfigBase.ConfigGroup;
import net.createmod.catnip.config.ConfigBase.ConfigInt;
import net.createmod.catnip.config.ui.ConfigAnnotations.IntDisplay;

public class CClient extends ConfigBase {
   public final ConfigGroup client = this.group(0, "client", new String[]{CClient.Comments.client});
   public final ConfigBool tooltips = this.b(true, "enableTooltips", new String[]{CClient.Comments.tooltips});
   public final ConfigBool enableOverstressedTooltip = this.b(true, "enableOverstressedTooltip", new String[]{CClient.Comments.enableOverstressedTooltip});
   public final ConfigBool explainRenderErrors = this.b(false, "explainRenderErrors", new String[]{CClient.Comments.explainRenderErrors});
   public final ConfigFloat fanParticleDensity = this.f(0.5F, 0.0F, 1.0F, "fanParticleDensity", new String[]{CClient.Comments.fanParticleDensity});
   public final ConfigFloat filterItemRenderDistance = this.f(10.0F, 1.0F, "filterItemRenderDistance", CClient.Comments.filterItemRenderDistance);
   public final ConfigInt mainMenuConfigButtonRow = this.i(2, 0, 4, "mainMenuConfigButtonRow", CClient.Comments.mainMenuConfigButtonRow);
   public final ConfigInt mainMenuConfigButtonOffsetX = this.i(
      -4, Integer.MIN_VALUE, Integer.MAX_VALUE, "mainMenuConfigButtonOffsetX", CClient.Comments.mainMenuConfigButtonOffsetX
   );
   public final ConfigInt ingameMenuConfigButtonRow = this.i(3, 0, 5, "ingameMenuConfigButtonRow", CClient.Comments.ingameMenuConfigButtonRow);
   public final ConfigInt ingameMenuConfigButtonOffsetX = this.i(
      -4, Integer.MIN_VALUE, Integer.MAX_VALUE, "ingameMenuConfigButtonOffsetX", CClient.Comments.ingameMenuConfigButtonOffsetX
   );
   public final ConfigBool ignoreFabulousWarning = this.b(false, "ignoreFabulousWarning", new String[]{CClient.Comments.ignoreFabulousWarning});
   public final ConfigBool rotateWhenSeated = this.b(true, "rotateWhenSeated", new String[]{CClient.Comments.rotatewhenSeated});
   public final ConfigGroup fluidFogSettings = this.group(1, "fluidFogSettings", new String[]{CClient.Comments.fluidFogSettings});
   public final ConfigFloat honeyTransparencyMultiplier = this.f(1.0F, 0.125F, 256.0F, "honey", new String[]{CClient.Comments.honeyTransparencyMultiplier});
   public final ConfigFloat chocolateTransparencyMultiplier = this.f(
      1.0F, 0.125F, 256.0F, "chocolate", new String[]{CClient.Comments.chocolateTransparencyMultiplier}
   );
   public final ConfigGroup overlay = this.group(1, "goggleOverlay", new String[]{CClient.Comments.overlay});
   public final ConfigInt overlayOffsetX = this.i(20, Integer.MIN_VALUE, Integer.MAX_VALUE, "overlayOffsetX", new String[]{CClient.Comments.overlayOffset});
   public final ConfigInt overlayOffsetY = this.i(0, Integer.MIN_VALUE, Integer.MAX_VALUE, "overlayOffsetY", new String[]{CClient.Comments.overlayOffset});
   public final ConfigBool overlayCustomColor = this.b(false, "customColorsOverlay", new String[]{CClient.Comments.overlayCustomColor});
   public final ConfigInt overlayBackgroundColor = this.i(
      -267386864, Integer.MIN_VALUE, Integer.MAX_VALUE, "customBackgroundOverlay", CClient.Comments.overlayBackgroundColor
   );
   public final ConfigInt overlayBorderColorTop = this.i(
      1347420415, Integer.MIN_VALUE, Integer.MAX_VALUE, "customBorderTopOverlay", CClient.Comments.overlayBorderColorTop
   );
   public final ConfigInt overlayBorderColorBot = this.i(
      1344798847, Integer.MIN_VALUE, Integer.MAX_VALUE, "customBorderBotOverlay", CClient.Comments.overlayBorderColorBot
   );
   public final ConfigGroup sound = this.group(1, "sound", new String[]{CClient.Comments.sound});
   public final ConfigBool enableAmbientSounds = this.b(true, "enableAmbientSounds", new String[]{CClient.Comments.enableAmbientSounds});
   public final ConfigFloat ambientVolumeCap = this.f(0.1F, 0.0F, 1.0F, "ambientVolumeCap", new String[]{CClient.Comments.ambientVolumeCap});
   public final ConfigGroup integration = this.group(1, "recipeViewerIntegration", new String[]{CClient.Comments.integration});
   public final ConfigEnum<StockKeeperRequestScreen.SearchSyncMode> syncRecipeViewerSearch = this.e(
      StockKeeperRequestScreen.SearchSyncMode.SYNC_BOTH, "syncRecipeViewerSearch", new String[]{CClient.Comments.syncRecipeViewerSearch}
   );
   public final ConfigGroup trains = this.group(1, "trains", new String[]{CClient.Comments.trains});
   public final ConfigFloat mountedZoomMultiplier = this.f(3.0F, 0.0F, "mountedZoomMultiplier", new String[]{CClient.Comments.mountedZoomMultiplier});
   public final ConfigBool showTrackGraphOnF3 = this.b(false, "showTrackGraphOnF3", new String[]{CClient.Comments.showTrackGraphOnF3});
   public final ConfigBool showExtendedTrackGraphOnF3 = this.b(false, "showExtendedTrackGraphOnF3", new String[]{CClient.Comments.showExtendedTrackGraphOnF3});
   public final ConfigBool showTrainMapOverlay = this.b(true, "showTrainMapOverlay", new String[]{CClient.Comments.showTrainMapOverlay});
   public final ConfigEnum<CClient.TrainMapTheme> trainMapColorTheme = this.e(
      CClient.TrainMapTheme.RED, "trainMapColorTheme", new String[]{CClient.Comments.trainMapColorTheme}
   );

   public String getName() {
      return "client";
   }

   private static class Comments {
      static String client = "Client-only settings - If you're looking for general settings, look inside your worlds serverconfig folder!";
      static String tooltips = "Show item descriptions on Shift and controls on Ctrl.";
      static String enableOverstressedTooltip = "Display a tooltip when looking at overstressed components.";
      static String explainRenderErrors = "Log a stack-trace when rendering issues happen within a moving contraption.";
      static String fanParticleDensity = "Higher density means more spawned particles.";
      static String[] filterItemRenderDistance = new String[]{
         "[in Blocks]", "Maximum Distance to the player at which items in Blocks' filter slots will be displayed"
      };
      static String[] mainMenuConfigButtonRow = new String[]{
         "Choose the menu row that the Create config button appears on in the main menu", "Set to 0 to disable the button altogether"
      };
      static String[] mainMenuConfigButtonOffsetX = new String[]{
         "Offset the Create config button in the main menu by this many pixels on the X axis",
         "The sign (-/+) of this value determines what side of the row the button appears on (left/right)"
      };
      static String[] ingameMenuConfigButtonRow = new String[]{
         "Choose the menu row that the Create config button appears on in the in-game menu", "Set to 0 to disable the button altogether"
      };
      static String[] ingameMenuConfigButtonOffsetX = new String[]{
         "Offset the Create config button in the in-game menu by this many pixels on the X axis",
         "The sign (-/+) of this value determines what side of the row the button appears on (left/right)"
      };
      static String ignoreFabulousWarning = "Setting this to true will prevent Create from sending you a warning when playing with Fabulous graphics enabled";
      static String rotatewhenSeated = "Disable to prevent being rotated while seated on a Moving Contraption";
      static String overlay = "Settings for the Goggle Overlay";
      static String overlayOffset = "Offset the overlay from goggle- and hover- information by this many pixels on the respective axis; Use /create overlay";
      static String overlayCustomColor = "Enable this to use your custom colors for the Goggle- and Hover- Overlay";
      static String[] overlayBackgroundColor = new String[]{
         "The custom background color to use for the Goggle- and Hover- Overlays, if enabled", "[in Hex: #AaRrGgBb]", IntDisplay.HEX.asComment()
      };
      static String[] overlayBorderColorTop = new String[]{
         "The custom top color of the border gradient to use for the Goggle- and Hover- Overlays, if enabled",
         "[in Hex: #AaRrGgBb]",
         IntDisplay.HEX.asComment()
      };
      static String[] overlayBorderColorBot = new String[]{
         "The custom bot color of the border gradient to use for the Goggle- and Hover- Overlays, if enabled",
         "[in Hex: #AaRrGgBb]",
         IntDisplay.HEX.asComment()
      };
      static String sound = "Sound settings";
      static String enableAmbientSounds = "Make cogs rumble and machines clatter.";
      static String ambientVolumeCap = "Maximum volume modifier of Ambient noise";
      static String trains = "Railway related settings";
      static String showTrainMapOverlay = "Display Track Networks and Trains on supported map mods";
      static String trainMapColorTheme = "Track Network Color on maps";
      static String mountedZoomMultiplier = "How far away the Camera should zoom when seated on a train";
      static String showTrackGraphOnF3 = "Display nodes and edges of a Railway Network while f3 debug mode is active";
      static String showExtendedTrackGraphOnF3 = "Additionally display materials of a Rail Network while f3 debug mode is active";
      static String fluidFogSettings = "Configure your vision range when submerged in Create's custom fluids";
      static String honeyTransparencyMultiplier = "The vision range through honey will be multiplied by this factor";
      static String chocolateTransparencyMultiplier = "The vision range though chocolate will be multiplied by this factor";
      static String integration = "Mod Integration and Recipe Viewer";
      static String syncRecipeViewerSearch = "How Recipe Viewer search should interact with Stock Keepers";
   }

   public static enum PlacementIndicatorSetting {
      TEXTURE,
      TRIANGLE,
      NONE;
   }

   public static enum TrainMapTheme {
      RED,
      GREY,
      WHITE;
   }
}
