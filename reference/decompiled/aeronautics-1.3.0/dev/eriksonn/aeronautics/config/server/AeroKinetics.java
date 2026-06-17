package dev.eriksonn.aeronautics.config.server;

import net.createmod.catnip.config.ConfigBase;

public class AeroKinetics extends ConfigBase {
   public final AeroStress stressValues = (AeroStress)this.nested(1, AeroStress::new, new String[]{AeroKinetics.Comments.stress});

   public String getName() {
      return "kinetics";
   }

   private static class Comments {
      static String stress = "Fine tune the kinetic stats of individual components";
   }
}
