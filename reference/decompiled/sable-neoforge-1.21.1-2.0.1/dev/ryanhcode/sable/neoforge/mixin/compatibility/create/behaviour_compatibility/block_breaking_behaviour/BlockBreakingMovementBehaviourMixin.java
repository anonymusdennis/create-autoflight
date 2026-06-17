package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.behaviour_compatibility.block_breaking_behaviour;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.block_breakers.SubLevelBlockBreakingUtility;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockBreakingMovementBehaviour.class})
public abstract class BlockBreakingMovementBehaviourMixin implements MovementBehaviour {
   @Shadow
   public abstract boolean canBreak(Level var1, BlockPos var2, BlockState var3);

   @WrapMethod(
      method = {"visitNewPosition"}
   )
   public void sable$checkPosition(MovementContext context, BlockPos pos, Operation<Void> original) {
      if (!context.stall) {
         original.call(new Object[]{context, pos});
         if (!context.stall) {
            Vec3 localCenter = context.localPos.getCenter();
            Vec3 sublevelLocalCenter = context.contraption.entity.toGlobalVector(localCenter, 1.0F);
            Vec3 subLevelLocalDir = context.rotation.apply(this.getActiveAreaOffset(context));
            BlockPos breakingPosWSublevel = SubLevelBlockBreakingUtility.findBreakingPos(
               (blockPos, state) -> this.canBreak(context.world, blockPos, state),
               Sable.HELPER.getContaining(context.world, context.contraption.anchor),
               context.world,
               subLevelLocalDir,
               sublevelLocalCenter,
               pos
            );
            original.call(new Object[]{context, breakingPosWSublevel});
         }
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void sable$testBreakingPosDist(MovementContext context, CallbackInfo ci) {
      CompoundTag data = context.data;
      if (data.contains("BreakingPos") || data.contains("LastPos")) {
         BlockPos blockPos = NbtUtils.readBlockPos(data, "BreakingPos").orElseGet(() -> (BlockPos)NbtUtils.readBlockPos(data, "LastPos").orElse(null));
         if (blockPos != null) {
            Vec3 localCenter = context.localPos.getCenter();
            Vec3 sublevelLocalCenter = context.contraption.entity.toGlobalVector(localCenter, 1.0F);
            Vec3 targetCenter = blockPos.getCenter();
            ActiveSableCompanion helper = Sable.HELPER;
            SubLevel parentSublevel = helper.getContaining(context.world, context.contraption.anchor);
            SubLevel targetSubLevel = helper.getContaining(context.world, blockPos);
            if (parentSublevel != null) {
               sublevelLocalCenter = parentSublevel.logicalPose().transformPosition(sublevelLocalCenter);
            }

            if (targetSubLevel != null) {
               targetCenter = targetSubLevel.logicalPose().transformPosition(targetCenter);
            }

            if (sublevelLocalCenter.distanceToSqr(targetCenter) > 4.0) {
               data.remove("Progress");
               data.remove("TicksUntilNextProgress");
               data.remove("BreakingPos");
               data.remove("LastPos");
               data.remove("WaitingTicks");
               context.stall = false;
               context.world.destroyBlockProgress(data.getInt("BreakerId"), blockPos, -1);
               ci.cancel();
            }
         }
      }
   }
}
