package dev.eriksonn.aeronautics.mixin.levitite;

import dev.eriksonn.aeronautics.content.components.Levitating;
import dev.eriksonn.aeronautics.index.AeroDataComponents;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ItemEntity.class})
public abstract class ItemEntityMixin extends Entity {
   public ItemEntityMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Shadow
   public abstract ItemStack getItem();

   @Inject(
      method = {"getDefaultGravity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void aeronautics$levitatingGravity(CallbackInfoReturnable<Double> cir) {
      Levitating component = (Levitating)this.getItem().get(AeroDataComponents.LEVITATING);
      if (component != null) {
         cir.setReturnValue(0.0);
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/item/ItemEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void aeronautics$levitatingDragAndSparkles(CallbackInfo ci) {
      Levitating component = (Levitating)this.getItem().get(AeroDataComponents.LEVITATING);
      if (component != null) {
         float dragFraction = Math.clamp(component.dragFraction(), 0.0F, 1.0F);
         this.setDeltaMovement(this.getDeltaMovement().scale((double)dragFraction));
         if (this.level().isClientSide
            && component.particle().isPresent()
            && this.level().random.nextFloat() < (float)Mth.clamp(this.getItem().getCount() - 10, 5, 100) / 64.0F) {
            Vec3 ppos = VecHelper.offsetRandomly(this.getPosition(0.0F), this.getRandom(), 0.4F).add(0.0, 0.3, 0.0);
            this.level().addParticle(component.particle().get(), ppos.x, ppos.y, ppos.z, 0.0, 0.0, 0.0);
         }
      }
   }
}
