package dev.eriksonn.aeronautics.config.server;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigFloat;

public class AeroPhysics extends ConfigBase {
   public final ConfigFloat mountedPotatoCannonMagnitude = this.f(
      0.2F, 0.0F, Float.MAX_VALUE, "recoil_magnitude", new String[]{AeroPhysics.Comments.mountedPotatoCannonComment}
   );
   public final ConfigFloat propellerBearingThrust = this.f(
      0.2F, 0.0F, Float.MAX_VALUE, "propellerBearingThrust", new String[]{AeroPhysics.Comments.propellerBearingThrust}
   );
   public final ConfigFloat propellerBearingAirflowMult = this.f(
      0.05F, 0.0F, Float.MAX_VALUE, "propellerBearingAirflow", new String[]{AeroPhysics.Comments.propellerBearingAirflow}
   );
   public final ConfigFloat woodenPropellerThrust = this.f(
      1.0F, 0.0F, Float.MAX_VALUE, "woodenPropellerThrust", new String[]{AeroPhysics.Comments.woodenPropellerThrust}
   );
   public final ConfigFloat woodenPropellerAirflow = this.f(
      0.1F, 0.0F, Float.MAX_VALUE, "woodenPropellerAirflow", new String[]{AeroPhysics.Comments.woodenPropellerAirflow}
   );
   public final ConfigFloat andesitePropellerThrust = this.f(
      1.0F, 0.0F, Float.MAX_VALUE, "andesitePropellerThrust", new String[]{AeroPhysics.Comments.andesitePropellerThrust}
   );
   public final ConfigFloat andesitePropellerAirflow = this.f(
      0.1F, 0.0F, Float.MAX_VALUE, "andesitePropellerAirflow", new String[]{AeroPhysics.Comments.andesitePropellerAirflow}
   );
   public final ConfigFloat smartPropellerThrust = this.f(
      1.0F, 0.0F, Float.MAX_VALUE, "smartPropellerThrust", new String[]{AeroPhysics.Comments.smartPropellerThrust}
   );
   public final ConfigFloat smartPropellerAirflow = this.f(
      0.1F, 0.0F, Float.MAX_VALUE, "smartPropellerAirflow", new String[]{AeroPhysics.Comments.smartPropellerAirflow}
   );
   public final ConfigFloat hotAirStrength = this.f(1.5F, 0.0F, Float.MAX_VALUE, "hotAirStrength", new String[]{AeroPhysics.Comments.hotAirStrength});
   public final ConfigFloat steamStrength = this.f(1.5F, 0.0F, Float.MAX_VALUE, "steamStrength", new String[]{AeroPhysics.Comments.steamStrength});

   public String getName() {
      return "physics";
   }

   private static class Comments {
      static String mountedPotatoCannonComment = "The recoil magnitude used whenever the Mounted Potato Cannon shoots";
      static String propellerBearingThrust = "Thrust scaling for Propeller Bearings";
      static String woodenPropellerThrust = "Thrust scaling for Wooden Propellers";
      static String woodenPropellerAirflow = "Airflow scaling for Wooden Propellers";
      static String andesitePropellerThrust = "Thrust scaling for Andesite Propellers";
      static String andesitePropellerAirflow = "Airflow scaling for Andesite Propellers";
      static String smartPropellerThrust = "Thrust scaling for Smart Propellers";
      static String smartPropellerAirflow = "Airflow scaling for Smart Propellers";
      static String propellerBearingAirflow = "Airflow scaling for Propeller Bearings";
      static String hotAirStrength = "kpg lifted per cubic meter of Hot Air";
      static String steamStrength = "kpg lifted per cubic meter of Steam";
   }
}
