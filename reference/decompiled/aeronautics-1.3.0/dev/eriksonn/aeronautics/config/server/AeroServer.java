package dev.eriksonn.aeronautics.config.server;

import net.createmod.catnip.config.ConfigBase;

public class AeroServer extends ConfigBase {
   public final AeroPhysics physics = (AeroPhysics)this.nested(0, AeroPhysics::new, new String[]{AeroServer.Comments.physics});
   public final AeroBlockConfigs blocks = (AeroBlockConfigs)this.nested(0, AeroBlockConfigs::new, new String[]{AeroServer.Comments.blockConfig});
   public final AeroKinetics kinetics = (AeroKinetics)this.nested(0, AeroKinetics::new, new String[]{AeroServer.Comments.kinetics});

   public String getName() {
      return "server";
   }

   private static class Comments {
      static String kinetics = "Parameters and abilities of Aeronautics's kinetic mechanisms";
      static String physics = "Parameters related to the physics of Aeronautics blocks";
      static String blockConfig = "Parameters and abilities of Aeronautics Blocks";
   }
}
