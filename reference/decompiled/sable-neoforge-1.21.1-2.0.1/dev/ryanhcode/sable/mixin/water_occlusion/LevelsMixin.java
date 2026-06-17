package dev.ryanhcode.sable.mixin.water_occlusion;

import dev.ryanhcode.sable.mixinterface.water_occlusion.WaterOcclusionContainerHolder;
import dev.ryanhcode.sable.platform.SablePlatform;
import dev.ryanhcode.sable.sublevel.water_occlusion.ClientWaterOcclusionContainer;
import dev.ryanhcode.sable.sublevel.water_occlusion.ServerWaterOcclusionContainer;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin({ServerLevel.class, ClientLevel.class})
public class LevelsMixin implements WaterOcclusionContainerHolder {
   @Unique
   private final WaterOcclusionContainer<?> sable$waterOcclusionContainer = this.sable$createWaterOcclusionContainer();

   @Unique
   private WaterOcclusionContainer<?> sable$createWaterOcclusionContainer() {
      Level self = (Level)this;
      if (SablePlatform.INSTANCE.isWrappedLevel(self)) {
         return null;
      } else {
         return (WaterOcclusionContainer<?>)(self.isClientSide ? ClientWaterOcclusionContainer.create(self) : ServerWaterOcclusionContainer.create(self));
      }
   }

   @Override
   public WaterOcclusionContainer<?> sable$getWaterOcclusionContainer() {
      return this.sable$waterOcclusionContainer;
   }
}
