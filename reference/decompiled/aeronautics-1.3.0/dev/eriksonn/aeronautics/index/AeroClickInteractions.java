package dev.eriksonn.aeronautics.index;

import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeClientCatalyzerHandler;
import dev.simulated_team.simulated.index.SimClickInteractions;

public class AeroClickInteractions extends SimClickInteractions {
   public static LevititeClientCatalyzerHandler LEVITITE_CATALYZER_HANDLER = (LevititeClientCatalyzerHandler)register(new LevititeClientCatalyzerHandler());

   public static void init() {
   }
}
