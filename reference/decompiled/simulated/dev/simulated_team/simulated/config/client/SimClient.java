package dev.simulated_team.simulated.config.client;

import dev.simulated_team.simulated.config.client.block.SimBlockConfigs;
import dev.simulated_team.simulated.config.client.items.SimItemConfigs;
import net.createmod.catnip.config.ConfigBase;
import org.jspecify.annotations.NonNull;

public class SimClient extends ConfigBase {
   public final SimItemConfigs itemConfig = (SimItemConfigs)this.nested(0, SimItemConfigs::new, new String[]{SimClient.Comments.itemConfig});
   public final SimBlockConfigs blockConfig = (SimBlockConfigs)this.nested(0, SimBlockConfigs::new, new String[]{SimClient.Comments.blockConfig});

   @NonNull
   public String getName() {
      return "client";
   }

   private static class Comments {
      static String itemConfig = "Settings of Simulated Items";
      static String blockConfig = "Settings of Simulated Blocks";
   }
}
