package dev.ryanhcode.sable.mixin.world_border;

import dev.ryanhcode.sable.mixinterface.world_border.WorldBorderExtension;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Level.class})
public class LevelMixin {
   @Shadow
   @Final
   private WorldBorder worldBorder;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void sable$initializeWorldBorder(
      WritableLevelData writableLevelData,
      ResourceKey resourceKey,
      RegistryAccess registryAccess,
      Holder holder,
      Supplier supplier,
      boolean bl,
      boolean bl2,
      long l,
      int i,
      CallbackInfo ci
   ) {
      ((WorldBorderExtension)this.worldBorder).sable$setLevel((Level)this);
   }
}
