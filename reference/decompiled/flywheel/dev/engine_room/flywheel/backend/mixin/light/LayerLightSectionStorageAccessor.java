package dev.engine_room.flywheel.backend.mixin.light;

import javax.annotation.Nullable;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({LayerLightSectionStorage.class})
public interface LayerLightSectionStorageAccessor {
   @Invoker("getDataLayer")
   @Nullable
   DataLayer flywheel$callGetDataLayer(long var1, boolean var3);
}
