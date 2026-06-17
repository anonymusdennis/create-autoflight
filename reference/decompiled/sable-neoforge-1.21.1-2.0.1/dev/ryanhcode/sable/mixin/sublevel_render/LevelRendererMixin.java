package dev.ryanhcode.sable.mixin.sublevel_render;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @Shadow
   @Nullable
   private ClientLevel level;

   @Inject(
      method = {"allChanged"},
      at = {@At("TAIL")}
   )
   private void sable$allChanged(CallbackInfo ci) {
      if (this.level != null) {
         SubLevelRenderDispatcher.get().rebuild(((ClientSubLevelContainer)((SubLevelContainerHolder)this.level).sable$getPlotContainer()).getAllSubLevels());
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;constantAmbientLight()Z",
         ordinal = 0,
         shift = Shift.BEFORE
      )}
   )
   private void sable$renderSingleBlockSubLevels(
      DeltaTracker deltaTracker,
      boolean bl,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f modelView,
      Matrix4f projection,
      CallbackInfo ci
   ) {
      Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer)((SubLevelContainerHolder)this.level).sable$getPlotContainer()).getAllSubLevels();
      Vec3 cameraPosition = camera.getPosition();
      SubLevelRenderDispatcher.get()
         .renderAfterSections(
            sublevels, cameraPosition.x, cameraPosition.y, cameraPosition.z, modelView, projection, deltaTracker.getGameTimeDeltaPartialTick(false)
         );
   }
}
