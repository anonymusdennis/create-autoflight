package dev.ryanhcode.sable.mixin.entity.teleport_players;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.RelativeMovement;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
   @Shadow
   public abstract ServerLevel serverLevel();

   @WrapMethod(
      method = {"teleportTo(DDD)V"}
   )
   public void sable$teleportTo(double x, double y, double z, Operation<Void> original) {
      Vector3d globalPos = Sable.HELPER.projectOutOfSubLevel(this.serverLevel(), new Vector3d(x, y, z));
      original.call(new Object[]{globalPos.x, globalPos.y, globalPos.z});
   }

   @WrapMethod(
      method = {"teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z"}
   )
   public boolean sable$teleportTo(
      ServerLevel serverLevel, double x, double y, double z, Set<RelativeMovement> set, float g, float h, Operation<Boolean> original
   ) {
      Vector3d globalPos = Sable.HELPER.projectOutOfSubLevel(serverLevel, new Vector3d(x, y, z));
      return (Boolean)original.call(new Object[]{serverLevel, globalPos.x, globalPos.y, globalPos.z, set, g, h});
   }
}
