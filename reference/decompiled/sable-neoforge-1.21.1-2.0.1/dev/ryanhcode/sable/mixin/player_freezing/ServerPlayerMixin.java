package dev.ryanhcode.sable.mixin.player_freezing;

import com.mojang.authlib.GameProfile;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin extends Player implements PlayerFreezeExtension {
   public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
      super(level, blockPos, f, gameProfile);
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void setSable$frozenToSubLevel(CallbackInfo ci) {
      this.sable$kick();
   }

   @Unique
   private void sable$kick() {
      UUID uuid = this.sable$getFrozenToSubLevel();
      if (uuid != null) {
         SubLevelContainer container = SubLevelContainer.getContainer(this.level());

         assert container != null;

         SubLevel subLevel = container.getSubLevel(uuid);
         if (subLevel != null) {
            ((EntityMovementExtension)this).sable$setTrackingSubLevel(subLevel);
            this.sable$teleport();
            this.sable$freezeTo(null, null);
         }
      }
   }
}
