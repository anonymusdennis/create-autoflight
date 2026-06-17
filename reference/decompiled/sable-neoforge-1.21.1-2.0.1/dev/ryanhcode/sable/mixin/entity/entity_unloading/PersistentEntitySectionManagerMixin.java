package dev.ryanhcode.sable.mixin.entity.entity_unloading;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PersistentEntitySectionManager.class})
public class PersistentEntitySectionManagerMixin {
   @Shadow
   @Final
   public EntitySectionStorage<EntityAccess> sectionStorage;

   @Inject(
      method = {"processChunkUnload"},
      at = {@At("HEAD")}
   )
   private void processChunkUnload(long l, CallbackInfoReturnable<Boolean> cir) {
      for (EntitySection<EntityAccess> section : this.sectionStorage.getExistingSectionsInChunk(l).toList()) {
         for (EntityAccess entityAccess : section.getEntities().toList()) {
            Entity entity = (Entity)entityAccess;
            boolean inPlot = SubLevelContainer.getContainer(entity.level()).inBounds(entity.chunkPosition());
            if (inPlot
               && (entity.getRemovalReason() == null || entity.getRemovalReason().shouldSave())
               && entity.isVehicle()
               && entity.hasExactlyOnePlayerPassenger()) {
               ((Entity)entity.getPassengers().getFirst()).removeVehicle();
            }
         }
      }
   }
}
