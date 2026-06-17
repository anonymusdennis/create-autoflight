package dev.ryanhcode.sable.mixin.punching;

import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.network.client.ClientSubLevelPunchHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MinecraftMixin {
   @Shadow
   @Nullable
   public LocalPlayer player;
   @Shadow
   @Nullable
   public ClientLevel level;

   @Shadow
   @Nullable
   public abstract ClientPacketListener getConnection();

   @Inject(
      method = {"startAttack"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"
      )}
   )
   private void tryPaddling(CallbackInfoReturnable<Boolean> cir) {
      if (this.player.getMainHandItem().is(SableTags.PADDLES) && !this.player.getCooldowns().isOnCooldown(this.player.getMainHandItem().getItem())) {
         BlockHitResult hitResult = ItemInvoker.sable$getPlayerPOVHitResult(this.level, this.player, Fluid.ANY);
         if (hitResult.getType() == Type.BLOCK) {
            FluidState state = this.level.getFluidState(hitResult.getBlockPos());
            if (!state.isEmpty()) {
               ClientSubLevelPunchHelper.clientTryPunch(hitResult, this.level, false);
            }
         }
      }
   }
}
