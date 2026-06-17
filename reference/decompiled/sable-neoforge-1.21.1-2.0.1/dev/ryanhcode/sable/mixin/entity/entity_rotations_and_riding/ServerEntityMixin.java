package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import dev.ryanhcode.sable.Sable;
import java.util.List;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerEntity.class})
public abstract class ServerEntityMixin {
   @Shadow
   private List<Entity> lastPassengers;
   @Shadow
   @Final
   private Entity entity;

   @Inject(
      method = {"sendChanges"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
      )}
   )
   private void sable$beforeSendChanges(CallbackInfo ci) {
      List<Entity> passengers = this.entity.getPassengers();
      if (Sable.HELPER.getContaining(this.entity) != null) {
         this.lastPassengers = passengers;
      }
   }
}
