package dev.ryanhcode.sable.neoforge.mixin.block_outline_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.block_outline_render.SubLevelCamera;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {LevelRenderer.class},
   priority = 2000
)
public abstract class LevelRendererMixin {
   @Unique
   private final Quaternionf sable$orientationStorage = new Quaternionf();
   @Unique
   private final SubLevelCamera sable$sublevelCamera = new SubLevelCamera();
   @Shadow
   @Nullable
   private ClientLevel level;

   @WrapOperation(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/neoforged/neoforge/client/ClientHooks;onDrawHighlight(Lnet/minecraft/client/renderer/LevelRenderer;Lnet/minecraft/client/Camera;Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/client/DeltaTracker;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)Z"
      )}
   )
   private boolean sable$preRenderHitOutline(
      LevelRenderer context,
      Camera camera,
      HitResult target,
      DeltaTracker deltaTracker,
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      Operation<Boolean> original,
      @Share("drawn") LocalBooleanRef drawnRef
   ) {
      if (target instanceof BlockHitResult blockTarget) {
         BlockPos blockPos = blockTarget.getBlockPos();
         ClientSubLevel subLevel = (ClientSubLevel)Sable.HELPER.getContaining(this.level, blockPos);
         if (subLevel == null) {
            return (Boolean)original.call(new Object[]{context, camera, target, deltaTracker, poseStack, bufferSource});
         } else {
            poseStack.pushPose();
            Pose3dc pose = subLevel.renderPose();
            this.sable$sublevelCamera.setCamera(camera);
            this.sable$sublevelCamera.setPose(pose);
            Vec3 cameraPosition = this.sable$sublevelCamera.getPosition();
            Vec3 realCameraPosition = camera.getPosition();
            Vector3dc position = pose.position();
            Vector3dc rotationPoint = pose.rotationPoint();
            Quaterniondc orientation = pose.orientation();
            Vector3dc scale = pose.scale();
            poseStack.translate(
               (float)(position.x() - realCameraPosition.x), (float)(position.y() - realCameraPosition.y), (float)(position.z() - realCameraPosition.z)
            );
            poseStack.mulPose(this.sable$orientationStorage.set(orientation));
            poseStack.translate(
               (float)(-(rotationPoint.x() - cameraPosition.x)),
               (float)(-(rotationPoint.y() - cameraPosition.y)),
               (float)(-(rotationPoint.z() - cameraPosition.z))
            );
            poseStack.scale((float)scale.x(), (float)scale.y(), (float)scale.z());
            drawnRef.set(true);
            return (Boolean)original.call(new Object[]{context, this.sable$sublevelCamera, target, deltaTracker, poseStack, bufferSource});
         }
      } else {
         return (Boolean)original.call(new Object[]{context, camera, target, deltaTracker, poseStack, bufferSource});
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/debug/DebugRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V"
      )}
   )
   public void sable$poseRenderHitOutline(CallbackInfo ci, @Local PoseStack poseStack, @Share("drawn") LocalBooleanRef drawnRef) {
      if (drawnRef.get()) {
         poseStack.popPose();
         this.sable$sublevelCamera.clear();
      }
   }

   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
      ),
      index = 3
   )
   public double modifyX(double original, @Share("drawn") LocalBooleanRef drawnRef) {
      return drawnRef.get() ? this.sable$sublevelCamera.getPosition().x : original;
   }

   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
      ),
      index = 4
   )
   public double modifyY(double original, @Share("drawn") LocalBooleanRef drawnRef) {
      return drawnRef.get() ? this.sable$sublevelCamera.getPosition().y : original;
   }

   @ModifyArg(
      method = {"renderLevel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;renderHitOutline(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"
      ),
      index = 5
   )
   public double modifyZ(double original, @Share("drawn") LocalBooleanRef drawnRef) {
      return drawnRef.get() ? this.sable$sublevelCamera.getPosition().z : original;
   }
}
