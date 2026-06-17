package dev.simulated_team.simulated.mixin.honey_glue;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.simulated_team.simulated.index.SimTags;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorStandItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ArmorStandItem.class})
public class ArmorStandItemMixin {
   @WrapOperation(
      method = {"useOn"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;isEmpty()Z"
      )}
   )
   private boolean ignoreGlues(List<Entity> instance, Operation<Boolean> original) {
      return instance.stream().filter(e -> !e.getType().is(SimTags.Misc.ARMOR_STAND_IGNORE)).toList().isEmpty();
   }
}
