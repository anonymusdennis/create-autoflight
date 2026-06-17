package dev.simulated_team.simulated.mixin.world_presets;

import com.llamalad7.mixinextras.sugar.Local;
import dev.simulated_team.simulated.content.worldgen.SimulatedWorldPreset;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import java.util.Optional;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.end.EndDragonFight.Data;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CreateWorldScreen.class})
public abstract class CreateWorldScreenMixin {
   @Shadow
   @Final
   WorldCreationUiState uiState;

   @Inject(
      method = {"createNewWorld"},
      at = {@At("HEAD")}
   )
   private void simulated$createNewWorld(CallbackInfo ci) {
      Holder<WorldPreset> holder = this.uiState.getWorldType().preset();
      if (holder != null) {
         Optional<ResourceKey<WorldPreset>> key = holder.unwrapKey();
         if (!key.isEmpty()) {
            ResourceLocation location = key.get().location();
            SimulatedWorldPreset simPreset = SimWorldPresets.PRESETS.get(location);
            if (simPreset != null) {
               GameRules gameRules = this.uiState.getGameRules();
               simPreset.modifyGameRules(gameRules);
            }
         }
      }
   }

   @Inject(
      method = {"createNewWorld"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Minecraft;createWorldOpenFlows()Lnet/minecraft/client/gui/screens/worldselection/WorldOpenFlows;",
         shift = Shift.BEFORE
      )}
   )
   private void simulated$createNewWorld2(CallbackInfo ci, @Local WorldData worldData) {
      Holder<WorldPreset> holder = this.uiState.getWorldType().preset();
      if (holder != null) {
         Optional<ResourceKey<WorldPreset>> key = holder.unwrapKey();
         if (!key.isEmpty()) {
            ((PrimaryLevelDataExtension)worldData).setPreset(key.get().location());
            if (holder.is(SimWorldPresets.END_SEA.id())) {
               ((PrimaryLevelDataExtension)worldData)
                  .setEndDragonFight(new Data(false, true, true, false, Optional.empty(), Optional.empty(), Optional.empty()));
            }
         }
      }
   }
}
