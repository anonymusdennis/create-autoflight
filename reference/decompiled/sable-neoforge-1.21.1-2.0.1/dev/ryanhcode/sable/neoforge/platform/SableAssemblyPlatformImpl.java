package dev.ryanhcode.sable.neoforge.platform;

import dev.ryanhcode.sable.platform.SableAssemblyPlatform;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SableAssemblyPlatformImpl implements SableAssemblyPlatform {
   @Override
   public void setIgnoreOnPlace(Level level, boolean ignore) {
      level.captureBlockSnapshots = ignore;
   }
}
