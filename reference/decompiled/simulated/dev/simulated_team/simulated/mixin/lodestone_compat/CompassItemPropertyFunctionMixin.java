package dev.simulated_team.simulated.mixin.lodestone_compat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.ClientLodestonePositions;
import dev.simulated_team.simulated.index.SimDataComponents;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({CompassItemPropertyFunction.class})
public abstract class CompassItemPropertyFunctionMixin {
   @Shadow
   protected abstract float getRotationTowardsCompassTarget(Entity var1, long var2, BlockPos var4);

   @Shadow
   protected abstract float getRandomlySpinningRotation(int var1, long var2);

   @WrapMethod(
      method = {"getCompassRotation"}
   )
   private float simulated$prioritizeID(ItemStack stack, ClientLevel level, int seed, Entity entity, Operation<Float> original) {
      if (stack.has(SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER)) {
         UUID trackerID = (UUID)stack.get(SimDataComponents.LODESTONE_COMPASS_SUBLEVEL_TRACKER);
         ClientLodestonePositions positions = (ClientLodestonePositions)ClientLodestonePositions.clientPositions.get(level);
         Vector3d pos = (Vector3d)positions.CLIENT_LODESTONE_MAP.get(trackerID);
         return pos != null
            ? this.getRotationTowardsCompassTarget(entity, level.getGameTime(), BlockPos.containing(pos.x, pos.y, pos.z))
            : this.getRandomlySpinningRotation(seed, level.getGameTime());
      } else {
         return (Float)original.call(new Object[]{stack, level, seed, entity});
      }
   }
}
