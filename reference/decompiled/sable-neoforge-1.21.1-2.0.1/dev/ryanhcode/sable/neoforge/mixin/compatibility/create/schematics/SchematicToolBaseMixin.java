package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.content.schematics.client.tools.SchematicToolBase;
import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.RaycastHelper.PredicateTraceResult;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SchematicToolBase.class})
public class SchematicToolBaseMixin {
   @Shadow
   protected Vec3 chasingSelectedPos;
   @Shadow
   protected Vec3 lastChasingSelectedPos;

   @Inject(
      method = {"updateSelection"},
      at = {@At("TAIL")}
   )
   public void sable$forceUpdateSelection(CallbackInfo ci, @Local(ordinal = 0) Vec3 target) {
      ActiveSableCompanion helper = Sable.HELPER;
      if (helper.getContainingClient(target) != helper.getContainingClient(this.lastChasingSelectedPos)) {
         this.lastChasingSelectedPos = this.chasingSelectedPos = target;
      }
   }

   @WrapOperation(
      method = {"updateTargetPos"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/foundation/utility/RaycastHelper;rayTraceUntil(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;)Lcom/simibubi/create/foundation/utility/RaycastHelper$PredicateTraceResult;"
      )}
   )
   public PredicateTraceResult sable$rayTraceSublevels(
      Vec3 start,
      Vec3 end,
      Predicate<BlockPos> predicate,
      Operation<PredicateTraceResult> original,
      @Local LocalPlayer player,
      @Local SchematicTransformation transformation
   ) {
      ClientSubLevel subLevel = Sable.HELPER.getContainingClient(transformation.getAnchor());
      if (subLevel != null) {
         Pose3dc pose = subLevel.renderPose();
         Vec3 plotPlayerPos = pose.transformPositionInverse(player.getEyePosition());
         Vec3 plotStart = transformation.toLocalSpace(plotPlayerPos);
         Vec3 plotEnd = transformation.toLocalSpace(RaycastHelper.getTraceTarget(player, 70.0, plotPlayerPos));
         return (PredicateTraceResult)original.call(new Object[]{plotStart, plotEnd, predicate});
      } else {
         return (PredicateTraceResult)original.call(new Object[]{start, end, predicate});
      }
   }
}
