package dev.eriksonn.aeronautics.index;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.Aeronautics;

public class AeroPartialModels {
   public static final PartialModel STEAM_VENT_REDSTONE = block("steam_vent/redstone");
   public static final PartialModel STEAM_VENT_BASE = block("steam_vent/steam_base");
   public static final PartialModel STEAM_VENT_JET = block("steam_vent/steam_jet");
   public static final PartialModel BEARING_PLATE = block("propeller_bearing/bearing_plate");
   public static final PartialModel BEARING_PLATE_METAL = block("gyroscopic_propeller_bearing/metal_bearing_plate");
   public static final PartialModel GYRO_BEARING_PISTON_HEAD = block("gyroscopic_propeller_bearing/piston_head");
   public static final PartialModel GYRO_BEARING_PISTON_POLE = block("gyroscopic_propeller_bearing/piston_pole");
   public static final PartialModel HOT_AIR_BURNER_INDICATOR = block("adjustable_burner/redstone_indicator");
   public static final PartialModel CANNON_BARREL = block("mounted_potato_cannon/partials/barrel");
   public static final PartialModel CANNON_BELLOW = block("mounted_potato_cannon/partials/bellow");
   public static final PartialModel CANNON_COG = block("mounted_potato_cannon/partials/cog");
   public static final PartialModel ANDESITE_PROPELLER = block("andesite_propeller/propeller");
   public static final PartialModel WOODEN_PROPELLER = block("wooden_propeller/propeller");
   public static final PartialModel ANDESITE_PROPELLER_REVERSED = block("andesite_propeller/propeller_reversed");
   public static final PartialModel WOODEN_PROPELLER_REVERSED = block("wooden_propeller/propeller_reversed");
   public static final PartialModel SMART_PROPELLER = block("smart_propeller/propeller");
   public static final PartialModel SMART_PROPELLER_REVERSED = block("smart_propeller/propeller_reversed");
   public static final PartialModel SMART_PROPELLER_HINGE = block("smart_propeller/hinge");

   private static PartialModel block(String path) {
      return PartialModel.of(Aeronautics.path("block/" + path));
   }

   private static PartialModel entity(String path) {
      return PartialModel.of(Aeronautics.path("entity/" + path));
   }

   private static PartialModel item(String path) {
      return PartialModel.of(Aeronautics.path("item/" + path));
   }

   public static void init() {
   }
}
