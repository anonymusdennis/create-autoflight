package dev.ryanhcode.sable.mixin.sculk_vibrations;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationInfo;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem.Ticker;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem.User;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Ticker.class})
public interface VibrationSystemTickerMixin {
   @WrapOperation(
      method = {"receiveVibration"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/gameevent/vibrations/VibrationInfo;pos()Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private static Vec3 sable$useGlobalPos(VibrationInfo instance, Operation<Vec3> original, @Local(argsOnly = true) ServerLevel level) {
      return Sable.HELPER.projectOutOfSubLevel(level, (Vec3)original.call(new Object[]{instance}));
   }

   @WrapOperation(
      method = {"receiveVibration", "lambda$trySelectAndScheduleVibration$0", "method_51408", "tryReloadVibrationParticle"},
      expect = 3,
      require = 3,
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/gameevent/vibrations/VibrationSystem$User;getPositionSource()Lnet/minecraft/world/level/gameevent/PositionSource;"
      )}
   )
   private static PositionSource sable$useGlobalDestPos(User instance, Operation<PositionSource> original, @Local(argsOnly = true) ServerLevel level) {
      PositionSource origSource = (PositionSource)original.call(new Object[]{instance});
      Optional<Vec3> optPos = origSource.getPosition(level);
      return (PositionSource)(optPos.isPresent()
         ? new BlockPositionSource(BlockPos.containing(Sable.HELPER.projectOutOfSubLevel(level, optPos.get())))
         : origSource);
   }
}
