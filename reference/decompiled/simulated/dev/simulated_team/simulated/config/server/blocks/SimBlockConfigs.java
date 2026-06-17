package dev.simulated_team.simulated.config.server.blocks;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigFloat;
import net.createmod.catnip.config.ConfigBase.ConfigInt;

public class SimBlockConfigs extends ConfigBase {
   public final ConfigInt opticalSensorRange = this.i(
      15, 0, Integer.MAX_VALUE, "optical_sensor_max_range", new String[]{"Maximum range for the Optical Sensor"}
   );
   public final ConfigInt laserPointerRange = this.i(100, 0, Integer.MAX_VALUE, "laser_pointer_max_range", new String[]{"Maximum range for the Laser Pointer"});
   public final ConfigFloat maxRopeRange = this.f(40.0F, 0.0F, 1000.0F, "max_rope_range", new String[]{"Maximum range for rope connections"});
   public final ConfigFloat maxRopeStretchAllowed = this.f(
      25.0F,
      0.0F,
      100.0F,
      "max_rope_winch_stretch_allowed",
      new String[]{"Maximum percent the rope mounted on a Rope Winch is allowed to stretch before not accepting input"}
   );
   public final ConfigFloat maxRopeZiplineAngle = this.f(
      85.0F, 0.0F, 90.0F, "max_rope_zipline_angle", new String[]{"Steepest angle at which a rope can be grabbed onto using a wrench in degrees"}
   );
   public final ConfigFloat maxSwivelBearingSpeed = this.f(
      96.0F, 0.0F, 256.0F, "max_swivel_bearing_speed", new String[]{"The maximum RPM a Swivel Bearing is allowed to rotate at"}
   );
   public final ConfigInt dockingConnectorFECapacity = this.i(
      10000, 0, Integer.MAX_VALUE, "docking_connector_fe_capacity", new String[]{"The maximum FE capacity of Docking Connectors"}
   );
   public final ConfigInt dockingConnectorFEThroughput = this.i(
      10000, 0, Integer.MAX_VALUE, "docking_connector_fe_throughput", new String[]{"The maximum FE/t throughput of Docking Connectors"}
   );

   public String getName() {
      return "blocks";
   }

   private static class Comments {
      private static final String opticalSensorRange = "Maximum range for the Optical Sensor";
      private static final String laserPointerRange = "Maximum range for the Laser Pointer";
      private static final String maxRopeRange = "Maximum range for rope connections";
      private static final String maxRopeWinchStretch = "Maximum percent the rope mounted on a Rope Winch is allowed to stretch before not accepting input";
      private static final String maxRopeZiplineAngle = "Steepest angle at which a rope can be grabbed onto using a wrench in degrees";
      private static final String maxSwivelBearingSpeed = "The maximum RPM a Swivel Bearing is allowed to rotate at";
      public static final String dockingConnectorFECapacity = "The maximum FE capacity of Docking Connectors";
      public static final String dockingConnectorFEThroughput = "The maximum FE/t throughput of Docking Connectors";
   }
}
