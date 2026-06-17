package dev.ryanhcode.sable.mixin.entity.tnt_jumps;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({PrimedTnt.class})
public abstract class PrimedTntMixin extends Entity {
   public PrimedTntMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/LivingEntity;)V"},
      at = {@At("TAIL")}
   )
   private void sable$setTntJump(Level level, double d, double e, double f, LivingEntity livingEntity, CallbackInfo ci) {
      SubLevel subLevel = Sable.HELPER.getContaining(level, this.blockPosition());
      if (subLevel != null) {
         this.setDeltaMovement(subLevel.logicalPose().transformNormalInverse(this.getDeltaMovement()));
      }
   }
}
