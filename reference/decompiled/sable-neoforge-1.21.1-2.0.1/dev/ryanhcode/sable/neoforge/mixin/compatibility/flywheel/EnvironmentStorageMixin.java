package dev.ryanhcode.sable.neoforge.mixin.compatibility.flywheel;

import dev.engine_room.flywheel.backend.engine.embed.EnvironmentStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({EnvironmentStorage.class})
public class EnvironmentStorageMixin {
   @ModifyArg(
      method = {"<init>"},
      at = @At(
         value = "INVOKE",
         target = "Ldev/engine_room/flywheel/backend/engine/CpuArena;<init>(JI)V"
      ),
      index = 0
   )
   private long sable$overrideMatrixSize(long elementSizeBytes) {
      return 192L;
   }
}
