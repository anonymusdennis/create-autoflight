package dev.simulated_team.simulated.mixin.spring_item_bounce;

import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Shadow
   private BlockPos blockPosition;
   @Shadow
   private Level level;

   @Shadow
   public abstract void playSound(SoundEvent var1, float var2, float var3);

   @Shadow
   public abstract BlockPos blockPosition();

   @Shadow
   public abstract Level level();

   @Shadow
   public abstract Vec3 getPosition(float var1);

   @Redirect(
      method = {"move"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"
      )
   )
   private void updateEntityAfterFallOn(Block instance, BlockGetter pLevel, Entity entity) {
      if (entity instanceof ItemEntity item && item.getItem().is((Item)SimItems.SPRING.get())) {
         entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, -1.0, 1.0));
         return;
      }

      instance.updateEntityAfterFallOn(pLevel, entity);
   }

   @Inject(
      method = {"checkFallDamage"},
      at = {@At("HEAD")}
   )
   private void awardAdvancementBeforeFallReset(double d, boolean bl, BlockState blockState, BlockPos blockPos, CallbackInfo ci) {
      if (bl) {
         Entity player = (Entity)this;
         if (player instanceof ItemEntity item
            && item.getItem().is((Item)SimItems.SPRING.get())
            && item.fallDistance >= 128.0F
            && item.getOwner() instanceof Player playerx) {
            SimAdvancements.MUST_COME_UP.awardTo(playerx);
         }
      }
   }
}
