package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.behaviour_compatibility.harvester_behaviour;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.behavior_compatibility.harvester_block_entity.DummyMovementContext;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({HarvesterMovementBehaviour.class})
public class HarvesterMovementBehaviourMixin {
   @WrapMethod(
      method = {"visitNewPosition"}
   )
   public void sable$checkAllPositions(MovementContext context, BlockPos pos, Operation<Void> original) {
      if (context instanceof DummyMovementContext) {
         original.call(new Object[]{context, pos});
      } else {
         ActiveSableCompanion helper = Sable.HELPER;
         helper.runIncludingSubLevels(context.world, pos.getCenter(), true, helper.getContaining(context.contraption.entity), (sublevel, blockPos) -> {
            original.call(new Object[]{context, blockPos});
            return null;
         });
      }
   }
}
