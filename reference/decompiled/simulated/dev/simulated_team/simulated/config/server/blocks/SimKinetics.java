package dev.simulated_team.simulated.config.server.blocks;

import net.createmod.catnip.config.ConfigBase;

public class SimKinetics extends ConfigBase {
   public final SimStress stressValues = (SimStress)this.nested(1, SimStress::new, new String[]{SimKinetics.Comments.stress});

   public String getName() {
      return "kinetics";
   }

   private static class Comments {
      static String stress = "Fine tune the kinetic stats of individual components";
   }
}
