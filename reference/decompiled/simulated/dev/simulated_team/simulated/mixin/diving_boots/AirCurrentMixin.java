package dev.simulated_team.simulated.mixin.diving_boots;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({AirCurrent.class})
public class AirCurrentMixin {
   @Shadow
   public Direction direction;

   @WrapMethod(
      method = {"isPlayerCreativeFlying"}
   )
   private static boolean simulated$testDivingBoots(Entity entity, Operation<Boolean> original) {
      return DivingBootsItem.isWornBy(entity) ? true : (Boolean)original.call(new Object[]{entity});
   }
}
