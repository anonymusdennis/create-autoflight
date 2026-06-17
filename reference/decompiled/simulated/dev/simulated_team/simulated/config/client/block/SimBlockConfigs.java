package dev.simulated_team.simulated.config.client.block;

import net.createmod.catnip.config.ConfigBase;
import net.createmod.catnip.config.ConfigBase.ConfigInt;
import org.jspecify.annotations.NonNull;

public class SimBlockConfigs extends ConfigBase {
   public final ConfigInt steeringWheelXOffset = this.i(0, -1000, 1000, "steering_wheel_x_offset", new String[]{SimBlockConfigs.Comments.steeringWheelXOffset});
   public final ConfigInt steeringWheelYOffset = this.i(0, -1000, 1000, "steering_wheel_y_offset", new String[]{SimBlockConfigs.Comments.steeringWheelYOffset});

   @NonNull
   public String getName() {
      return "block";
   }

   public static class Comments {
      static String steeringWheelXOffset = "Offsets the Steering Wheel GUI on the X axis.";
      static String steeringWheelYOffset = "Offsets the Steering Wheel GUI on the Y axis.";
   }
}
