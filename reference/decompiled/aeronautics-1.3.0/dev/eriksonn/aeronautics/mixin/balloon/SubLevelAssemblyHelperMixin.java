package dev.eriksonn.aeronautics.mixin.balloon;

import com.llamalad7.mixinextras.sugar.Local;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SubLevelAssemblyHelper.class})
public class SubLevelAssemblyHelperMixin {
   @Inject(
      method = {"needsBitSet"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void aeronautics$needsBitSet(ServerLevel level, BoundingBox3ic bounds, List<Entity> entities, CallbackInfoReturnable<Boolean> cir) {
      BalloonMap balloonMap = (BalloonMap)BalloonMap.MAP.get(level);

      for (Balloon balloon : balloonMap.getBalloons()) {
         if (balloon.getBounds().intersects(bounds)) {
            cir.setReturnValue(true);
            return;
         }
      }
   }

   @Inject(
      method = {"moveOtherStuff"},
      at = {@At("TAIL")}
   )
   private static void aeronautics$assemble(
      ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> blocks, BoundingBox3ic bounds, CallbackInfo ci, @Local BoundedBitVolume3i volume
   ) {
      BalloonMap balloonMap = (BalloonMap)BalloonMap.MAP.get(level);

      for (Balloon balloon : balloonMap.getBalloons()) {
         if (balloon.getBounds().intersects(bounds)) {
            boolean shouldMoveBalloon = false;

            for (Direction direction : SimDirectionUtil.VALUES) {
               BlockPos relativePos = balloon.getControllerPos().relative(direction);
               if (volume.getOccupied(relativePos.getX(), relativePos.getY(), relativePos.getZ())) {
                  shouldMoveBalloon = true;
                  break;
               }
            }

            if (shouldMoveBalloon) {
               balloon.setAssembling(transform);
            }
         }
      }
   }
}
