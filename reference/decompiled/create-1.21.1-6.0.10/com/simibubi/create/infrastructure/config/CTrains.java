package com.simibubi.create.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.createmod.catnip.config.ConfigBase.ConfigFloat;
import net.createmod.catnip.config.ConfigBase.ConfigGroup;
import net.createmod.catnip.config.ConfigBase.ConfigInt;

public class CTrains extends ConfigBase {
   public final ConfigBool trainsCauseDamage = this.b(true, "trainsCauseDamage", new String[]{CTrains.Comments.trainsCauseDamage});
   public final ConfigInt maxTrackPlacementLength = this.i(32, 16, 128, "maxTrackPlacementLength", new String[]{CTrains.Comments.maxTrackPlacementLength});
   public final ConfigInt maxAssemblyLength = this.i(128, 5, 512, "maxAssemblyLength", new String[]{CTrains.Comments.maxAssemblyLength});
   public final ConfigInt maxBogeyCount = this.i(20, 1, 200, "maxBogeyCount", new String[]{CTrains.Comments.maxBogeyCount});
   public final ConfigFloat manualTrainSpeedModifier = this.f(0.75F, 0.0F, "manualTrainSpeedModifier", new String[]{CTrains.Comments.manualTrainSpeedModifier});
   public final ConfigGroup trainStats = this.group(1, "trainStats", new String[]{"Standard Trains"});
   public final ConfigFloat trainTopSpeed = this.f(28.0F, 0.0F, "trainTopSpeed", new String[]{CTrains.Comments.mps, CTrains.Comments.trainTopSpeed});
   public final ConfigFloat trainTurningTopSpeed = this.f(
      14.0F, 0.0F, "trainTurningTopSpeed", new String[]{CTrains.Comments.mps, CTrains.Comments.trainTurningTopSpeed}
   );
   public final ConfigFloat trainAcceleration = this.f(3.0F, 0.0F, "trainAcceleration", new String[]{CTrains.Comments.acc, CTrains.Comments.trainAcceleration});
   public final ConfigGroup poweredTrainStats = this.group(1, "poweredTrainStats", new String[]{"Powered Trains"});
   public final ConfigFloat poweredTrainTopSpeed = this.f(
      40.0F, 0.0F, "poweredTrainTopSpeed", new String[]{CTrains.Comments.mps, CTrains.Comments.poweredTrainTopSpeed}
   );
   public final ConfigFloat poweredTrainTurningTopSpeed = this.f(
      20.0F, 0.0F, "poweredTrainTurningTopSpeed", new String[]{CTrains.Comments.mps, CTrains.Comments.poweredTrainTurningTopSpeed}
   );
   public final ConfigFloat poweredTrainAcceleration = this.f(
      3.0F, 0.0F, "poweredTrainAcceleration", new String[]{CTrains.Comments.acc, CTrains.Comments.poweredTrainAcceleration}
   );

   public String getName() {
      return "trains";
   }

   private static class Comments {
      static String mps = "[in Blocks/Second]";
      static String acc = "[in Blocks/Second²]";
      static String trainTopSpeed = "The top speed of any assembled Train.";
      static String trainTurningTopSpeed = "The top speed of Trains during a turn.";
      static String trainAcceleration = "The acceleration of any assembled Train.";
      static String poweredTrainTopSpeed = "The top speed of powered Trains.";
      static String poweredTrainTurningTopSpeed = "The top speed of powered Trains during a turn.";
      static String poweredTrainAcceleration = "The acceleration of powered Trains.";
      static String trainsCauseDamage = "Whether moving Trains can hurt colliding mobs and players.";
      static String maxTrackPlacementLength = "Maximum length of track that can be placed as one batch or turn.";
      static String maxAssemblyLength = "Maximum length of a Train Stations' assembly track.";
      static String maxBogeyCount = "Maximum amount of bogeys assembled as a single Train.";
      static String manualTrainSpeedModifier = "Relative speed of a manually controlled Train compared to a Scheduled one.";
   }
}
