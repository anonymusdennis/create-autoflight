package net.createmod.ponder;

import java.util.Random;
import net.createmod.catnip.lang.LangBuilder;
import net.createmod.catnip.net.CatnipPackets;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Ponder {
   public static final String MOD_ID = "ponder";
   public static final String MOD_NAME = "Ponder";
   public static final Logger LOGGER = LogManager.getLogger("Ponder");
   public static final Random RANDOM = new Random();

   public static LangBuilder lang() {
      return new LangBuilder("ponder");
   }

   public static ResourceLocation asResource(String path) {
      return ResourceLocation.fromNamespaceAndPath("ponder", path);
   }

   public static void init() {
      CatnipPackets.register();
   }
}
