package dev.ryanhcode.sable.mixin.block_decal_render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public abstract class LevelRendererMixin {
   @Unique
   private final Quaternionf sable$orientationStorage = new Quaternionf();
   @Shadow
   @Nullable
   private ClientLevel level;

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;last()Lcom/mojang/blaze3d/vertex/PoseStack$Pose;",
         shift = Shift.BEFORE
      )}
   )
   private void sable$preRenderBlockDamage(
      DeltaTracker deltaTracker,
      boolean bl,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci,
      @Local(ordinal = 0) PoseStack ps,
      @Local(ordinal = 0) BlockPos pos
   ) {
      Vec3 plotPos = new Vec3((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      ClientSubLevel subLevel = (ClientSubLevel)Sable.HELPER.getContaining(this.level, plotPos);
      if (subLevel != null) {
         Pose3dc renderPose = subLevel.renderPose();
         Vec3 cameraPos = camera.getPosition();
         Vec3 projectedPos = renderPose.transformPosition(plotPos);
         ps.popPose();
         ps.pushPose();
         ps.translate(projectedPos.x - cameraPos.x, projectedPos.y - cameraPos.y, projectedPos.z - cameraPos.z);
         ps.mulPose(this.sable$orientationStorage.set(renderPose.orientation()));
      }
   }

   @ModifyConstant(
      method = {"renderLevel"},
      constant = {@Constant(
         doubleValue = 1024.0,
         ordinal = 0
      )}
   )
   private double sable$blockDamageDistance(double originalBlockDamageDistanceConstant) {
      return Double.MAX_VALUE;
   }
}
