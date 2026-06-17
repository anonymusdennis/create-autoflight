package dev.ryanhcode.sable.mixin.recoil;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ProjectileDispenseBehavior.class})
public class ProjectileDispenseBehaviorMixin {
   @Inject(
      method = {"execute"},
      at = {@At("TAIL")}
   )
   private void sable$applyRecoil(
      BlockSource blockSource, ItemStack itemStack, CallbackInfoReturnable<ItemStack> cir, @Local Position position, @Local Direction direction
   ) {
      ServerLevel level = blockSource.level();
      if (Sable.HELPER.getContaining(level, position) instanceof ServerSubLevel serverSubLevel) {
         Vector3d impulse = new Vector3d((double)direction.getStepX(), (double)direction.getStepY(), (double)direction.getStepZ()).mul(-1.5);
         RigidBodyHandle.of(serverSubLevel).applyImpulseAtPoint(JOMLConversion.toJOML(position), impulse);
      }
   }
}
