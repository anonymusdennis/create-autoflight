package net.createmod.catnip.platform.services;

import java.util.List;
import java.util.function.Supplier;
import net.createmod.catnip.platform.Env;
import net.createmod.catnip.platform.Loader;

public interface PlatformHelper {
   Loader getLoader();

   Env getEnv();

   boolean isModLoaded(String var1);

   boolean isDevelopmentEnvironment();

   List<String> getLoadedMods();

   String getModDisplayName(String var1);

   void executeOnClientOnly(Supplier<Runnable> var1);

   void executeOnServerOnly(Supplier<Runnable> var1);
}
