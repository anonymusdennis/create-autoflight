package com.simibubi.create.infrastructure.config;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigBool;
import net.createmod.catnip.config.ConfigBase.ConfigInt;

public class CLogistics extends ConfigBase {
   public final ConfigInt defaultExtractionTimer = this.i(8, 1, "defaultExtractionTimer", new String[]{CLogistics.Comments.defaultExtractionTimer});
   public final ConfigInt psiTimeout = this.i(60, 1, "psiTimeout", new String[]{CLogistics.Comments.psiTimeout});
   public final ConfigInt mechanicalArmRange = this.i(5, 1, "mechanicalArmRange", new String[]{CLogistics.Comments.mechanicalArmRange});
   public final ConfigInt packagePortRange = this.i(5, 1, "packagePortRange", new String[]{CLogistics.Comments.packagePortRange});
   public final ConfigInt linkRange = this.i(256, 1, "linkRange", new String[]{CLogistics.Comments.linkRange});
   public final ConfigInt displayLinkRange = this.i(64, 1, "displayLinkRange", new String[]{CLogistics.Comments.displayLinkRange});
   public final ConfigInt vaultCapacity = this.i(20, 1, 2048, "vaultCapacity", new String[]{CLogistics.Comments.vaultCapacity});
   public final ConfigInt chainConveyorCapacity = this.i(20, 1, "chainConveyorCapacity", new String[]{CLogistics.Comments.chainConveyorCapacity});
   public final ConfigInt brassTunnelTimer = this.i(10, 1, 10, "brassTunnelTimer", new String[]{CLogistics.Comments.brassTunnelTimer});
   public final ConfigInt factoryGaugeTimer = this.i(100, 5, "factoryGaugeTimer", new String[]{CLogistics.Comments.factoryGaugeTimer});
   public final ConfigBool seatHostileMobs = this.b(true, "seatHostileMobs", new String[]{CLogistics.Comments.seatHostileMobs});

   public String getName() {
      return "logistics";
   }

   private static class Comments {
      static String defaultExtractionTimer = "The amount of ticks a funnel waits between item transferrals, when it is not re-activated by redstone.";
      static String linkRange = "Maximum possible range in blocks of redstone link connections.";
      static String displayLinkRange = "Maximum possible distance in blocks between display links and their target.";
      static String psiTimeout = "The amount of ticks a portable storage interface waits for transfers until letting contraptions move along.";
      static String mechanicalArmRange = "Maximum distance in blocks a Mechanical Arm can reach across.";
      static String packagePortRange = "Maximum distance in blocks a Package Port can be placed at from its target.";
      static String vaultCapacity = "The total amount of stacks a vault can hold per block in size.";
      static String chainConveyorCapacity = "The amount of packages a chain conveyor can carry at a time.";
      static String brassTunnelTimer = "The amount of ticks a brass tunnel waits between distributions.";
      static String factoryGaugeTimer = "The amount of ticks a factory gauge waits between requests.";
      static String seatHostileMobs = "Whether hostile mobs walking near a seat will start riding it.";
   }
}
