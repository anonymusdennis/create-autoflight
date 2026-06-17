package dev.ryanhcode.sable.mixin.death_message;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CombatTracker.class})
public class CombatTrackerMixin {
   @Shadow
   @Final
   private LivingEntity mob;

   @WrapOperation(
      method = {"getFallMessage"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
      )}
   )
   private MutableComponent sable$getFallMessage(String string, Object[] objects, Operation<MutableComponent> original) {
      LivingEntity entity = this.mob;
      SubLevel subLevel = Sable.HELPER.getLastTrackingSubLevel(entity);
      return subLevel != null && subLevel.getName() != null && !subLevel.getName().isEmpty() && Sable.HELPER.getTrackingSubLevel(entity) != subLevel
         ? Component.translatable("death.attack.fall.from_sublevel", new Object[]{entity.getDisplayName(), subLevel.getName()})
         : (MutableComponent)original.call(new Object[]{string, objects});
   }
}
