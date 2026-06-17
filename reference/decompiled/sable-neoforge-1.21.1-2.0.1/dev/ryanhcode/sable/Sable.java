package dev.ryanhcode.sable;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelTicketLoadingSystem;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.network.tcp.SableTCPPackets;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertiesDefinitionLoader;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointObserver;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

public final class Sable {
   public static final String MOD_NAME = "Sable";
   public static final String MOD_ID = "sable";
   public static final String ISSUE_TRACKER_URL = "https://github.com/ryanhcode/sable/issues";
   public static final Logger LOGGER = LogUtils.getLogger();
   public static final ActiveSableCompanion HELPER = (ActiveSableCompanion)SableCompanion.INSTANCE;
   private static final List<String> WITTIER_COMMENTS = List.of(
      "Hi. I'm Sable and I dislike float casts",
      "*plays dead*",
      "It wasn't me (it probably was)",
      "Lets see if this is repro or cosmic radiation",
      "What did you do",
      "ooprs",
      "dude... thats so mossed up...",
      "What is this thing",
      "I am capable of so much more than being a crash log. There has to be more to this world.",
      "tfw no sable gf",
      "someone please advice devs that pancakes are serve"
   );

   @Internal
   public static void init() {
      SableTCPPackets.init();
      SableTags.register();
      PhysicsBlockPropertyTypes.register();
      ForceGroups.register();
      LOGGER.info("{} loaded!", "Sable");
   }

   public static ResourceLocation sablePath(String path) {
      return ResourceLocation.fromNamespaceAndPath("sable", path);
   }

   @Internal
   public static void defaultSubLevelContainerInitializer(Level level, SubLevelContainer container) {
      if (container instanceof ServerSubLevelContainer serverContainer) {
         ServerLevel serverLevel = serverContainer.getLevel();
         SubLevelPhysicsSystem physicsSystem = new SubLevelPhysicsSystem(serverLevel);
         physicsSystem.initialize();
         serverContainer.takePhysicsSystem(physicsSystem);
         SubLevelTrackingSystem trackingSystem = new SubLevelTrackingSystem(serverLevel);
         serverContainer.takeTrackingSystem(trackingSystem);
         serverContainer.addObserver(physicsSystem);
         serverContainer.addObserver(trackingSystem);
         serverContainer.addObserver(new SubLevelTrackingPointObserver(serverLevel));
         serverContainer.addObserver(new SubLevelTicketLoadingSystem(serverContainer));
         PhysicsBlockPropertiesDefinitionLoader.INSTANCE.applyAll();
      }
   }

   private static String getWittierComment() {
      try {
         return LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY && Util.getMillis() % 2L == 0L
            ? "It's sable sunday"
            : WITTIER_COMMENTS.get((int)(Util.getMillis() % (long)WITTIER_COMMENTS.size()));
      } catch (Throwable var1) {
         return "Wittier comment unavailable :(";
      }
   }

   public static String getCrashHeader() {
      return "\n// "
         + getWittierComment()
         + "\nPlease make sure this issue is not caused by Sable before reporting it to other mod authors.\nIf you cannot reproduce it without Sable, file a report on the issue tracker\nhttps://github.com/ryanhcode/sable/issues\n";
   }
}
