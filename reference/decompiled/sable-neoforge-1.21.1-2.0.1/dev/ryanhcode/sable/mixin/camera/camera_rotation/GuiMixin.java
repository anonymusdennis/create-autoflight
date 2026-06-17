package dev.ryanhcode.sable.mixin.camera.camera_rotation;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Gui.class})
public class GuiMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"renderCrosshair"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/systems/RenderSystem;getModelViewStack()Lorg/joml/Matrix4fStack;"
      )}
   )
   private void sable$onRenderCrosshair(CallbackInfo ci, @Share("mountedOrientation") LocalRef<Quaterniond> mountedOrientation) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      Entity entity = camera.getEntity();
      float pt = this.minecraft.getTimer().getGameTimeDeltaPartialTick(true);
      Quaterniond ridingOrientation = EntitySubLevelRotationHelper.getEntityOrientation(
         entity, x -> ((ClientSubLevel)x).renderPose(), pt, EntitySubLevelRotationHelper.Type.CAMERA
      );
      mountedOrientation.set(ridingOrientation);
   }

   @Redirect(
      method = {"renderCrosshair"},
      at = @At(
         value = "INVOKE",
         target = "Lorg/joml/Matrix4fStack;rotateX(F)Lorg/joml/Matrix4f;"
      )
   )
   private Matrix4f sable$redirectRotateX(Matrix4fStack stack, float angle, @Share("mountedOrientation") LocalRef<Quaterniond> mountedOrientation) {
      if (mountedOrientation.get() != null) {
         float pt = this.minecraft.getTimer().getGameTimeDeltaPartialTick(true);
         Camera camera = this.minecraft.gameRenderer.getMainCamera();
         Entity entity = camera.getEntity();
         return stack.rotateX(-entity.getViewXRot(pt) * (float) (Math.PI / 180.0));
      } else {
         return stack.rotateX(angle);
      }
   }

   @Redirect(
      method = {"renderCrosshair"},
      at = @At(
         value = "INVOKE",
         target = "Lorg/joml/Matrix4fStack;rotateY(F)Lorg/joml/Matrix4f;"
      )
   )
   private Matrix4f sable$redirectRotateY(Matrix4fStack stack, float angle, @Share("mountedOrientation") LocalRef<Quaterniond> mountedOrientation) {
      if (mountedOrientation.get() != null) {
         float pt = this.minecraft.getTimer().getGameTimeDeltaPartialTick(true);
         Camera camera = this.minecraft.gameRenderer.getMainCamera();
         Entity entity = camera.getEntity();
         stack.rotateY(entity.getViewYRot(pt) * (float) (Math.PI / 180.0));
         return stack.rotate(new Quaternionf((Quaterniondc)mountedOrientation.get()).conjugate());
      } else {
         return stack.rotateY(angle);
      }
   }
}
