package dev.ryanhcode.sable.mixin.player_freezing;

import com.mojang.authlib.GameProfile;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.UUID;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LocalPlayer.class})
public abstract class LocalPlayerMixin extends Player implements PlayerFreezeExtension {
   public LocalPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
      super(level, blockPos, f, gameProfile);
   }

   @Redirect(
      method = {"tick"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;hasChunkAt(II)Z"
      )
   )
   private boolean sable$freezeTicking(Level instance, int x, int z) {
      this.sable$tickStopFreezing();
      UUID uuid = this.sable$getFrozenToSubLevel();
      if (uuid != null) {
         SubLevelContainer container = SubLevelContainer.getContainer(this.level());

         assert container != null;

         ClientSubLevel subLevel = (ClientSubLevel)container.getSubLevel(uuid);
         if (subLevel == null || !subLevel.isFinalized()) {
            return false;
         }

         this.sable$teleport();
         this.sable$freezeTo(null, null);
      }

      return instance.hasChunkAt(x, z);
   }
}
