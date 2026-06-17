package dev.simulated_team.simulated.config.server;

import dev.simulated_team.simulated.config.server.blocks.SimAssembly;
import dev.simulated_team.simulated.config.server.blocks.SimBlockConfigs;
import dev.simulated_team.simulated.config.server.blocks.SimKinetics;
import dev.simulated_team.simulated.config.server.items.SimEquipment;
import dev.simulated_team.simulated.config.server.physics.SimPhysics;
import net.createmod.catnip.config.ConfigBase;

public class SimServer extends ConfigBase {
   public final SimKinetics kinetics = (SimKinetics)this.nested(0, SimKinetics::new, new String[]{SimServer.Comments.kinetics});
   public final SimAssembly assembly = (SimAssembly)this.nested(0, SimAssembly::new, new String[]{SimServer.Comments.assembly});
   public final SimPhysics physics = (SimPhysics)this.nested(0, SimPhysics::new, new String[]{SimServer.Comments.physics});
   public final SimBlockConfigs blocks = (SimBlockConfigs)this.nested(0, SimBlockConfigs::new, new String[]{SimServer.Comments.blockConfig});
   public final SimEquipment equipment = (SimEquipment)this.nested(0, SimEquipment::new, new String[]{SimServer.Comments.equipmentConfig});

   public String getName() {
      return "server";
   }

   private static class Comments {
      static String kinetics = "Parameters and abilities of Simulated's kinetic mechanisms";
      static String assembly = "Settings for sub-level assembly";
      static String physics = "Parameters related to the physics of Simulated Contraptions";
      static String blockConfig = "Parameters and abilities of Simulated Blocks";
      static String equipmentConfig = "Equipment and gadgets added by Simulated";
   }
}
