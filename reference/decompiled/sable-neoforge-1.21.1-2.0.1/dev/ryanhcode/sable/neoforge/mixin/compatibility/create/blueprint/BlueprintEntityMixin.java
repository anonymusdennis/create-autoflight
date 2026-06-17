package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.blueprint;

import com.simibubi.create.content.equipment.blueprint.BlueprintEntity;
import dev.ryanhcode.sable.annotation.MixinModVersionConstraint;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@MixinModVersionConstraint("(,6.0.11)")
@Mixin({BlueprintEntity.class})
public abstract class BlueprintEntityMixin extends Entity {
   public BlueprintEntityMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Overwrite
   public boolean canPlayerUse(Player player) {
      return player.canInteractWithEntity(this, 8.0);
   }
}
