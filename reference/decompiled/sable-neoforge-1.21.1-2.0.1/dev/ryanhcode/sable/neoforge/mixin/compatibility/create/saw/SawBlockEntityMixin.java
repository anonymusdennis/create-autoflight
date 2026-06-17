package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.saw;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({SawBlockEntity.class})
public abstract class SawBlockEntityMixin extends BlockBreakingKineticBlockEntity {
   public SawBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Redirect(
      method = {"dropItemFromCutTree"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;atLowerCornerOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private Vec3 sable$itemDeltaMovement(Vec3i vec3i) {
      ActiveSableCompanion helper = Sable.HELPER;
      Vector3d diff = helper.projectOutOfSubLevel(this.level, JOMLConversion.atCenterOf(this.breakingPos))
         .sub(helper.projectOutOfSubLevel(this.level, JOMLConversion.atCenterOf(this.worldPosition)));
      SubLevel subLevel = helper.getContaining(this);
      if (subLevel != null) {
         subLevel.logicalPose().transformNormalInverse(diff);
      }

      return JOMLConversion.toMojang(diff);
   }
}
