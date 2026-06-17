package dev.ryanhcode.sable.platform;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface SableAssemblyPlatform {
   SableAssemblyPlatform INSTANCE = SablePlatformUtil.load(SableAssemblyPlatform.class);

   void setIgnoreOnPlace(Level var1, boolean var2);
}
