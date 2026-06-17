package dev.simulated_team.simulated.index;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.simulated_team.simulated.Simulated;

public class SimPartialModels {
   public static final PartialModel QUARTER_SHAFT = block("quarter_shaft");
   public static final PartialModel SWIVEL_BEARING_COG = block("swivel_bearing/ironcog");
   public static final PartialModel MODULATING_RECEIVER_PLATE = block("modulating_linked_receiver/gold_plate");
   public static final PartialModel ROPE_WINCH_SHAFT = block("rope_winch/shaft");
   public static final PartialModel ROPE_WINCH_ROPE_COIL = block("rope_winch/rope_coil");
   public static final PartialModel ROPE_CONNECTOR_KNOT = block("rope_connector/knot");
   public static final PartialModel ASSEMBLER_LEVER = block("physics_assembler/lever");
   public static final PartialModel REDSTONE_INDUCTOR_INDICATOR = block("redstone_inductor/redstone_indicator");
   public static final PartialModel REDSTONE_ACCUMULATOR_DIODE = block("redstone_accumulator/diode");
   public static final PartialModel ALTITUDE_SENSOR_RADIAL_HAND = block("altitude_sensor/radial_hand");
   public static final PartialModel ALTITUDE_SENSOR_LINEAR_HAND = block("altitude_sensor/linear_hand");
   public static final PartialModel ALTITUDE_SENSOR_RADIAL_CASE = block("altitude_sensor/radial_case");
   public static final PartialModel ALTITUDE_SENSOR_LINEAR_CASE = block("altitude_sensor/linear_case");
   public static final PartialModel ALTITUDE_SENSOR_INDICATOR = block("altitude_sensor/indicator");
   public static final PartialModel ANALOG_TRANSMISSION_COG = block("analog_transmission/gear");
   public static final PartialModel STEERING_WHEEL = block("steering_wheel/wheel");
   public static final PartialModel SHAFT_SIXTEENTH = block("pedal_shaft/shaft_sixteenth");
   public static final PartialModel AUGER_COG = block("auger_shaft/cog");
   public static final PartialModel AUGER_REDSTONE_ON = block("auger_shaft/redstone_top_on");
   public static final PartialModel AUGER_REDSTONE_OFF = block("auger_shaft/redstone_top_off");
   public static final PartialModel THROTTLE_LEVER_BUTTON = block("throttle_lever/button");
   public static final PartialModel THROTTLE_LEVER_HANDLE = block("throttle_lever/handle");
   public static final PartialModel THROTTLE_LEVER_DIODE = block("throttle_lever/diode");
   public static final PartialModel VELOCITY_SENSOR_FAN = block("velocity_sensor/fan");
   public static final PartialModel VELOCITY_SENSOR_DIODE = block("velocity_sensor/diode");
   public static final PartialModel NAV_TABLE_POINTER = block("navigation_table/nav_table_pointer");
   public static final PartialModel NAV_TABLE_INDICATOR = block("navigation_table/redstone_indicator");
   public static final PartialModel GIMBAL_SENSOR_GIMBAL = block("gimbal_sensor/gimbal");
   public static final PartialModel GIMBAL_SENSOR_COMPASS = block("gimbal_sensor/compass");
   public static final PartialModel GIMBAL_SENSOR_NEEDLE = block("gimbal_sensor/needle");
   public static final PartialModel GIMBAL_SENSOR_INDICATOR = block("gimbal_sensor/redstone_indicator");
   public static final PartialModel CONTRAPTION_DIAGRAM_1x1 = entity("contraption_diagram_small");
   public static final PartialModel CONTRAPTION_DIAGRAM_2x2 = entity("contraption_diagram_medium");
   public static final PartialModel CONTRAPTION_DIAGRAM_3x3 = entity("contraption_diagram_large");
   public static final PartialModel SPRING_MIDDLE = block("spring/middle");
   public static final PartialModel TORSION_SPRING = block("torsion_spring/spring");
   public static final PartialModel ROPE = block("rope/rope");
   public static final PartialModel ROPE_KNOT = block("rope/knot");
   public static final PartialModel LINKED_TYPEWRITER_KEY = block("linked_typewriter/key");
   public static final PartialModel LINKED_TYPEWRITER_KEY_SPACEBAR = block("linked_typewriter/key_spacebar");
   public static final PartialModel LASER_POINTER_LENS_OFF = block("laser_pointer/lens_off");
   public static final PartialModel LASER_POINTER_LENS_ON = block("laser_pointer/lens_on");
   public static final PartialModel PHYSICS_STAFF_CORE_GLOW = item("creative_physics_staff/core_glow");
   public static final PartialModel PHYSICS_STAFF_CORE = item("creative_physics_staff/core");
   public static final PartialModel PHYSICS_STAFF_RING = item("creative_physics_staff/ring");
   public static final PartialModel PHYSICS_STAFF_SIGMA = item("creative_physics_staff/sigma");
   public static final PartialModel PHYSICS_STAFF_INNER_CUBE = item("creative_physics_staff/inner_cube");
   public static final PartialModel PHYSICS_STAFF_OUTER_CUBE = item("creative_physics_staff/outer_cube");
   public static final PartialModel DOCKING_CONNECTOR_MAIN_PISTON_BOTTOM = block("docking_connector/main_piston_1");
   public static final PartialModel DOCKING_CONNECTOR_MAIN_PISTON_TOP = block("docking_connector/main_piston_2");
   public static final PartialModel DOCKING_CONNECTOR_SIDE_PISTON_BOTTOM = block("docking_connector/side_piston_1");
   public static final PartialModel DOCKING_CONNECTOR_SIDE_PISTON_TOP = block("docking_connector/side_piston_2");
   public static final PartialModel DOCKING_CONNECTOR_FOOT = block("docking_connector/foot");
   public static final PartialModel ABSORBER_HAT = block("absorber/hat");
   public static final PartialModel ABSORBER_PIVOT = block("absorber/pivot");
   public static final PartialModel ABSORBER_ARM = block("absorber/arm");
   public static final PartialModel ABSORBER_SPONGE_DRY = block("absorber/sponge_dry");
   public static final PartialModel ABSORBER_SPONGE_WET = block("absorber/sponge_wet");
   public static final PartialModel DIRECTIONAL_GEARSHIFT_CENTER = block("directional_gearshift/barrel");
   public static final PartialModel DIRECTIONAL_GEARSHIFT_BARREL_SHAFT = block("directional_gearshift/barrel_shaft");
   public static final PartialModel LAUNCHED_PLUNGER_SPOOL = item("plunger_launcher/tether_spool");
   public static final PartialModel LAUNCHED_PLUNGER_JOINT = item("plunger_launcher/spool_joint");
   public static final PartialModel LAUNCHED_PLUNGER_BODY = item("plunger_launcher/plunger_tether");
   public static final SimPartialModels.EngineParts ENGINE_PARTS = new SimPartialModels.EngineParts("");
   public static final SimPartialModels.EngineParts ENGINE_PARTS_HEATED = new SimPartialModels.EngineParts("heated/");
   public static final SimPartialModels.EngineParts ENGINE_PARTS_SUPERHEATED = new SimPartialModels.EngineParts("superheated/");

   private static PartialModel block(String path) {
      return PartialModel.of(Simulated.path("block/" + path));
   }

   private static PartialModel entity(String path) {
      return PartialModel.of(Simulated.path("entity/" + path));
   }

   private static PartialModel item(String path) {
      return PartialModel.of(Simulated.path("item/" + path));
   }

   public static void init() {
   }

   public static class EngineParts {
      public final PartialModel pipeLeft;
      public final PartialModel pipeRight;
      public final PartialModel outletLeft;
      public final PartialModel outletRight;
      public final PartialModel hatchBottom;
      public final PartialModel hatchTop;
      public final PartialModel mouth;

      public EngineParts(String prefix) {
         this.pipeLeft = SimPartialModels.block("portable_engine/" + prefix + "exhaust_pipe_left");
         this.pipeRight = SimPartialModels.block("portable_engine/" + prefix + "exhaust_pipe_right");
         this.outletLeft = SimPartialModels.block("portable_engine/" + prefix + "exhaust_outlet_left");
         this.outletRight = SimPartialModels.block("portable_engine/" + prefix + "exhaust_outlet_right");
         this.hatchBottom = SimPartialModels.block("portable_engine/" + prefix + "hatch_bottom");
         this.hatchTop = SimPartialModels.block("portable_engine/" + prefix + "hatch_top");
         this.mouth = SimPartialModels.block("portable_engine/" + prefix + "mouth");
      }
   }
}
