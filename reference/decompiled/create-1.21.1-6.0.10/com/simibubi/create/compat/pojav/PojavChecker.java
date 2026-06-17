package com.simibubi.create.compat.pojav;

import java.util.regex.Pattern;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ScreenEvent.Init.Post;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PojavChecker {
   private static final Logger LOGGER = LoggerFactory.getLogger(PojavChecker.class);
   private static final Pattern KNOWN_ANDROID_PATH = Pattern.compile("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch");
   public static final boolean IS_PRESENT = (Boolean)Util.make(() -> {
      if (System.getenv("POJAV_RENDERER") != null) {
         LOGGER.warn("[Create]: Detected presence of environment variable POJAV_LAUNCHER, which seems to indicate we are running on Android");
         return true;
      } else {
         String librarySearchPaths = System.getProperty("java.library.path", null);
         if (librarySearchPaths != null) {
            for (String path : librarySearchPaths.split(":")) {
               if (isKnownAndroidPathFragment(path)) {
                  LOGGER.warn("[Create]: Found a library search path which seems to be hosted in an Android filesystem: {}", path);
                  return true;
               }
            }
         }

         String workingDirectory = System.getProperty("user.home", null);
         if (workingDirectory != null && isKnownAndroidPathFragment(workingDirectory)) {
            LOGGER.warn("[Create]: Working directory seems to be hosted in an Android filesystem: {}", workingDirectory);
            return true;
         } else {
            return false;
         }
      }
   });
   private static boolean screenShown = false;

   public static void init() {
      if (IS_PRESENT) {
         NeoForge.EVENT_BUS.addListener(PojavChecker::onScreenInit);
      }
   }

   public static void onScreenInit(Post event) {
      if (!screenShown && event.getScreen() instanceof TitleScreen titleScreen) {
         Minecraft.getInstance().setScreen(new PojavWarningScreen(titleScreen));
         screenShown = true;
      }
   }

   private static boolean isKnownAndroidPathFragment(String path) {
      return KNOWN_ANDROID_PATH.matcher(path).matches();
   }
}
