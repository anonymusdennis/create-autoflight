package dev.ryanhcode.sable.mixin.tracking_points;

import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
   @Shadow
   public abstract ServerLevel serverLevel();

   @Inject(
      method = {"addAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void addAdditionalSaveData(CompoundTag compoundTag, CallbackInfo ci) {
      SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(this.serverLevel());
      UUID loginPointUUID = data.generateTrackingPoint((ServerPlayer)this);
      if (loginPointUUID != null) {
         compoundTag.putUUID("LoginPoint", loginPointUUID);
      }
   }
}
