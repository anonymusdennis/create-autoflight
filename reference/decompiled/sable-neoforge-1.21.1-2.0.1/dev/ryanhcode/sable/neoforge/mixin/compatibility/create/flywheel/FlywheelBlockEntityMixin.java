package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.flywheel;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelReactionWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FlywheelBlockEntity.class})
public abstract class FlywheelBlockEntityMixin extends KineticBlockEntity implements BlockEntitySubLevelReactionWheel {
   @Unique
   float sable$smoothedSpeed = 0.0F;

   public FlywheelBlockEntityMixin(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
      super(arg, arg2, arg3);
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   public void sable$tick(CallbackInfo ci) {
      this.sable$smoothedSpeed = this.sable$smoothedSpeed + (this.speed - this.sable$smoothedSpeed) / 32.0F;
   }

   @Inject(
      method = {"write"},
      at = {@At("TAIL")}
   )
   public void sable$write(CompoundTag compound, Provider registries, boolean clientPacket, CallbackInfo ci) {
      compound.putFloat("SmoothedSpeed", this.sable$smoothedSpeed);
   }

   @Inject(
      method = {"read"},
      at = {@At("TAIL")}
   )
   public void sable$read(CompoundTag compound, Provider registries, boolean clientPacket, CallbackInfo ci) {
      this.sable$smoothedSpeed = compound.getFloat("SmoothedSpeed");
   }

   @Override
   public void sable$getAngularVelocity(Vector3d v) {
      Axis axis = ((IRotate)this.getBlockState().getBlock()).getRotationAxis(this.getBlockState());
      Direction dir = Direction.get(AxisDirection.NEGATIVE, axis);
      float angularSpeed = this.sable$smoothedSpeed * (float) (Math.PI * 2) / 60.0F;
      v.set((double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ()).mul((double)angularSpeed);
   }
}
