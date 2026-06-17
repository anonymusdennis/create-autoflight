package dev.simulated_team.simulated.mixin.world_presets;

import dev.simulated_team.simulated.content.worldgen.SimulatedWorldPreset;
import dev.simulated_team.simulated.index.SimWorldPresets;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout.RowHelper;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.WorldTypeEntry;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(
   targets = {"net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab"}
)
public class WorldTabMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")},
      remap = false,
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   private void simulated$init(CreateWorldScreen createWorldScreen, CallbackInfo ci, RowHelper rowHelper, CycleButton<WorldCreationUiState> cycleButton) {
      createWorldScreen.getUiState().addListener(worldCreationUiState -> {
         WorldTypeEntry worldType = worldCreationUiState.getWorldType();
         Holder<WorldPreset> preset = worldType.preset();
         if (preset != null && preset.unwrapKey().isPresent()) {
            ResourceKey<WorldPreset> key = (ResourceKey<WorldPreset>)preset.unwrapKey().get();
            SimulatedWorldPreset simPreset = SimWorldPresets.PRESETS.get(key.location());
            if (simPreset != null && simPreset.description() != null) {
               cycleButton.setTooltip(Tooltip.create(simPreset.description()));
            }
         }
      });
   }
}
