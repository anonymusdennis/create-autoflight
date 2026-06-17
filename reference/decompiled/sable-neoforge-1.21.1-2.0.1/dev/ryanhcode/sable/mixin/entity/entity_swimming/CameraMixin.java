package dev.ryanhcode.sable.mixin.entity.entity_swimming;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.client.Camera.NearPlane;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   private Vec3 position;
   @Shadow
   private BlockGetter level;

   @Shadow
   public abstract NearPlane getNearPlane();

   @Inject(
      method = {"getFluidInCamera"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void sable$getFluidInCamera(CallbackInfoReturnable<FogType> cir) {
      if (cir.getReturnValue() == FogType.NONE) {
         BoundingBox3d bounds = new BoundingBox3d(
            this.position.x - 0.5, this.position.y - 0.5, this.position.z - 0.5, this.position.x + 0.5, this.position.y + 0.5, this.position.z + 0.5
         );

         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting((Level)this.level, bounds)) {
            FogType fogType = this.sable$getFluidInCameraAt(((ClientSubLevel)subLevel).renderPose());
            if (fogType != null) {
               cir.setReturnValue(fogType);
               return;
            }
         }
      }
   }

   @Unique
   private FogType sable$getFluidInCameraAt(Pose3dc pose) {
      Vec3 localPosition = pose.transformPositionInverse(this.position);
      BlockPos localBlockPosition = BlockPos.containing(localPosition);
      FluidState fluidState = this.level.getFluidState(localBlockPosition);
      if (fluidState.is(FluidTags.WATER) && localPosition.y < (double)((float)localBlockPosition.getY() + fluidState.getHeight(this.level, localBlockPosition))
         )
       {
         return FogType.WATER;
      } else {
         NearPlane nearPlane = this.getNearPlane();

         for (Vec3 planeDir : Arrays.asList(
            nearPlane.getPointOnPlane(0.0F, 0.0F), nearPlane.getTopLeft(), nearPlane.getTopRight(), nearPlane.getBottomLeft(), nearPlane.getBottomRight()
         )) {
            Vec3 localPos = pose.transformPositionInverse(this.position.add(planeDir));
            BlockPos blockPos = BlockPos.containing(localPos);
            FluidState fluidState2 = this.level.getFluidState(blockPos);
            if (fluidState2.is(FluidTags.LAVA)) {
               if (localPos.y <= (double)(fluidState2.getHeight(this.level, blockPos) + (float)blockPos.getY())) {
                  return FogType.LAVA;
               }
            } else {
               BlockState blockState = this.level.getBlockState(blockPos);
               if (blockState.is(Blocks.POWDER_SNOW)) {
                  return FogType.POWDER_SNOW;
               }
            }
         }

         return FogType.NONE;
      }
   }
}
