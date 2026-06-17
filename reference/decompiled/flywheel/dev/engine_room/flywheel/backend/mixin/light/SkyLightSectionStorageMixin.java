package dev.engine_room.flywheel.backend.mixin.light;

import dev.engine_room.flywheel.backend.SkyLightSectionStorageExtension;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({SkyLightSectionStorage.class})
public abstract class SkyLightSectionStorageMixin extends LayerLightSectionStorage implements SkyLightSectionStorageExtension {
   protected SkyLightSectionStorageMixin() {
      super(null, null, null);
   }

   @Nullable
   @Override
   public DataLayer flywheel$skyDataLayer(long section) {
      long l = section;
      int i = SectionPos.y(section);
      SkyDataLayerStorageMapAccessor skyDataLayerStorageMap = (SkyDataLayerStorageMapAccessor)this.visibleSectionData;
      int j = skyDataLayerStorageMap.flywheel$topSections().get(SectionPos.getZeroNode(section));
      if (j != skyDataLayerStorageMap.flywheel$currentLowestY() && i < j) {
         DataLayer dataLayer = this.getDataLayerData(section);
         if (dataLayer == null) {
            while (dataLayer == null) {
               if (++i >= j) {
                  return null;
               }

               l = SectionPos.offset(l, Direction.UP);
               dataLayer = this.getDataLayerData(l);
            }
         }

         return dataLayer;
      } else {
         return null;
      }
   }
}
