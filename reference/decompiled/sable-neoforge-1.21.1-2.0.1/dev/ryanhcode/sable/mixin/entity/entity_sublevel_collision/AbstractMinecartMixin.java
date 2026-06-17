package dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractMinecart.class})
public abstract class AbstractMinecartMixin extends Entity {
   public AbstractMinecartMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void sable$postTick(CallbackInfo ci) {
      if (this.getType().is(SableTags.DESTROY_WHEN_LEAVING_PLOT)) {
         SubLevel containingSubLevel = Sable.HELPER.getContaining(this);
         if (containingSubLevel != null && !this.getBoundingBox().intersects(containingSubLevel.getPlot().getBoundingBox().toAABB().inflate(0.5))) {
            this.kill();
         }
      }
   }
}
