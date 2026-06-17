package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.nozzle.block_entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.fan.NozzleBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.NozzleBlockEntityExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({NozzleBlockEntity.class})
public abstract class NozzleBEFixesMixin extends SmartBlockEntity {
   public NozzleBEFixesMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Redirect(
      remap = false,
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/math/VecHelper;getCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   public Vec3 sable$nozzlePosition(Vec3i pos) {
      return JOMLConversion.toMojang(Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(pos)));
   }

   @Redirect(
      remap = false,
      method = {"lazyTick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/math/VecHelper;getCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   public Vec3 sable$nozzlePositionLazy(Vec3i pos) {
      return JOMLConversion.toMojang(Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(pos)));
   }

   @Redirect(
      method = {"canSee"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/math/VecHelper;getCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"
      ),
      remap = false
   )
   private Vec3 sable$projectCenter(Vec3i pos) {
      return JOMLConversion.toMojang(Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(pos)));
   }

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;clamp(III)I"
      )}
   )
   public int sable$clampParticlesMore(int value, int min, int max, Operation<Integer> original) {
      return (Integer)original.call(new Object[]{value, 3, max});
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
      )
   )
   public void sable$checkDirection(
      Level instance,
      ParticleOptions particleOptions,
      double x,
      double y,
      double z,
      double mx,
      double my,
      double mz,
      @Local(ordinal = 0) Vec3 origin,
      @Local(ordinal = 1) Vec3 start
   ) {
      Vec3 direction = start.subtract(origin).normalize();
      Direction nearest = Direction.getNearest(direction.x, direction.y, direction.z);
      if (((NozzleBlockEntityExtension)this).sable$getValidDirections().contains(nearest)) {
         instance.addParticle(particleOptions, x, y, z, mx, my, mz);
      }
   }
}
