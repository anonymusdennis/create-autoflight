package dev.ryanhcode.sable.mixin.entity.entity_collision;

import dev.ryanhcode.sable.mixinterface.entity.entity_collision.EntityExtension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({CollisionContext.class})
public interface CollisionContextMixin {
   @Shadow
   static CollisionContext empty() {
      return null;
   }

   @Overwrite
   static CollisionContext of(Entity entity) {
      return (CollisionContext)(entity != null ? ((EntityExtension)entity).sable$getCollisionContext() : empty());
   }
}
