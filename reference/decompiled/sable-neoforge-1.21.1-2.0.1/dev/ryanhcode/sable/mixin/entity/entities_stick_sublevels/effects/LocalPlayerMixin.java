package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels.effects;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerMixin extends Entity {
   public LocalPlayerMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Inject(
      method = {"playSound"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$playSound(SoundEvent soundEvent, float f, float g, CallbackInfo ci) {
      if (EntitySubLevelUtil.hasCustomEntityOrientation(this)) {
         Vector3d feet = Sable.HELPER.getFeetPos(this, 0.0F);
         this.level().playSound(null, feet.x, feet.y, feet.z, soundEvent, this.getSoundSource(), f, g);
         ci.cancel();
      }
   }
}
