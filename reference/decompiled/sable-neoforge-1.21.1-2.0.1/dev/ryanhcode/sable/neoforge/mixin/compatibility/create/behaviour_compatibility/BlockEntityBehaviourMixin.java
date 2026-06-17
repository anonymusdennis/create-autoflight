package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.behaviour_compatibility;

import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction.EdgeInteractionBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockEntityBehaviour.class})
public abstract class BlockEntityBehaviourMixin {
   @Shadow
   public static <T extends BlockEntityBehaviour> T get(BlockEntity be, BehaviourType<T> type) {
      return null;
   }

   @Inject(
      method = {"get(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lcom/simibubi/create/foundation/blockEntity/behaviour/BehaviourType;)Lcom/simibubi/create/foundation/blockEntity/behaviour/BlockEntityBehaviour;"},
      at = {@At("HEAD")},
      remap = false,
      cancellable = true
   )
   private static <T extends BlockEntityBehaviour> void sable$accountForSubLevels(
      BlockGetter reader, BlockPos pos, BehaviourType<T> type, CallbackInfoReturnable<T> cir
   ) {
      if (reader instanceof Level level && sable$checkType(type)) {
         ActiveSableCompanion helper = Sable.HELPER;
         BlockEntity caughtBE = helper.runIncludingSubLevels(
            level, pos.getCenter(), true, helper.getContaining(level, pos), (subLevel, internalPos) -> level.getBlockEntity(internalPos)
         );
         if (caughtBE != null) {
            cir.setReturnValue(get(caughtBE, type));
         }
      }
   }

   @Unique
   private static boolean sable$checkType(BehaviourType<?> type) {
      return type == BeltProcessingBehaviour.TYPE
         || type == DirectBeltInputBehaviour.TYPE
         || type == TransportedItemStackHandlerBehaviour.TYPE
         || type == InvManipulationBehaviour.TYPE
         || type == EdgeInteractionBehaviour.TYPE;
   }
}
