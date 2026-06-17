package dev.simulated_team.simulated.mixin.throttle_lever;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverClientGripHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameRenderer.class})
public class GameRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"pick(F)V"},
      at = {@At("TAIL")}
   )
   private void simulated$pickThrottleLever(float partialTicks, CallbackInfo ci) {
      if (this.minecraft != null) {
         LocalPlayer player = this.minecraft.player;
         if (player != null) {
            Vec3 eyePos = Sable.HELPER.getEyePositionInterpolated(player, partialTicks);
            HitResult mcHitResult = this.minecraft.hitResult;
            double minDistance = mcHitResult != null && mcHitResult.getType() != Type.MISS
               ? Sable.HELPER.distanceSquaredWithSubLevels(player.level(), eyePos, mcHitResult.getLocation())
               : Double.MAX_VALUE;

            for (ThrottleLeverBlockEntity lever : ThrottleLeverClientGripHandler.getNearbyThrottleLevers()) {
               if (!lever.isRemoved()) {
                  Double hitResultDistance = ThrottleLeverClientGripHandler.raycastLever(eyePos, player.getViewVector(partialTicks), lever, partialTicks);
                  if (hitResultDistance != null && hitResultDistance < minDistance) {
                     minDistance = hitResultDistance;
                     this.minecraft.hitResult = new BlockHitResult(lever.getBlockPos().getCenter(), Direction.UP, lever.getBlockPos(), false);
                  }
               }
            }
         }
      }
   }
}
