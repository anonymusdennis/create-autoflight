package com.simibubi.create.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.createmod.catnip.config.ConfigBase.ConfigGroup;
import net.createmod.catnip.config.ConfigBase.ConfigInt;

public class CSchematics extends ConfigBase {
   public final ConfigBool creativePrintIncludesAir = this.b(false, "creativePrintIncludesAir", new String[]{CSchematics.Comments.creativePrintIncludesAir});
   public final ConfigInt maxSchematics = this.i(10, 1, "maxSchematics", new String[]{CSchematics.Comments.maxSchematics});
   public final ConfigInt maxTotalSchematicSize = this.i(256, 16, "maxTotalSchematicSize", new String[]{CSchematics.Comments.kb, CSchematics.Comments.maxSize});
   public final ConfigInt maxSchematicPacketSize = this.i(
      1024, 256, 32767, "maxSchematicPacketSize", new String[]{CSchematics.Comments.b, CSchematics.Comments.maxPacketSize}
   );
   public final ConfigInt schematicIdleTimeout = this.i(600, 100, "schematicIdleTimeout", new String[]{CSchematics.Comments.idleTimeout});
   public final ConfigGroup schematicannon = this.group(0, "schematicannon", new String[]{"Schematicannon"});
   public final ConfigInt schematicannonDelay = this.i(10, 1, "schematicannonDelay", new String[]{CSchematics.Comments.delay});
   public final ConfigInt schematicannonShotsPerGunpowder = this.i(
      400, 1, "schematicannonShotsPerGunpowder", new String[]{CSchematics.Comments.schematicannonShotsPerGunpowder}
   );

   public String getName() {
      return "schematics";
   }

   private static class Comments {
      static String kb = "[in KiloBytes]";
      static String b = "[in Bytes]";
      static String maxSchematics = "The amount of Schematics a player can upload until previous ones are overwritten.";
      static String maxSize = "The maximum allowed file size of uploaded Schematics.";
      static String maxPacketSize = "The maximum packet size uploaded Schematics are split into.";
      static String idleTimeout = "Amount of game ticks without new packets arriving until an active schematic upload process is discarded.";
      static String delay = "Amount of game ticks between shots of the cannon. Higher => Slower";
      static String schematicannonShotsPerGunpowder = "Amount of blocks a Schematicannon can print per Gunpowder item provided.";
      static String creativePrintIncludesAir = "Whether placing a Schematic directly in Creative Mode should replace world blocks with Air";
   }
}
