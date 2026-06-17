package dev.ryanhcode.sable.mixin.sublevel_render.block_entity_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.VanillaSubLevelBlockEntityRenderer;
import dev.ryanhcode.sable.mixinterface.BlockEntityRenderDispatcherExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.List;
import java.util.SortedSet;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @Shadow
   @Nullable
   private ClientLevel level;
   @Shadow
   @Final
   private BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   @Shadow
   @Final
   private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;
   @Unique
   private VanillaSubLevelBlockEntityRenderer sable$subLevelBlockEntityRenderer;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   public void init(
      Minecraft minecraft,
      EntityRenderDispatcher entityRenderDispatcher,
      BlockEntityRenderDispatcher blockEntityRenderDispatcher,
      RenderBuffers renderBuffers,
      CallbackInfo ci
   ) {
      this.sable$subLevelBlockEntityRenderer = new VanillaSubLevelBlockEntityRenderer(blockEntityRenderDispatcher, renderBuffers, this.destructionProgress);
   }

   @WrapOperation(
      method = {"renderLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
         ordinal = 1
      )}
   )
   public <E extends BlockEntity> void sable$renderBlockEntities(
      BlockEntityRenderDispatcher instance,
      E blockEntity,
      float pt,
      PoseStack poseStack,
      MultiBufferSource multiBufferSource,
      Operation<Void> original,
      @Local Camera camera
   ) {
      ClientSubLevel subLevel = Sable.HELPER.getContainingClient(blockEntity);
      if (subLevel == null) {
         original.call(new Object[]{instance, blockEntity, pt, poseStack, multiBufferSource});
      } else {
         BlockEntityRenderDispatcherExtension extension = (BlockEntityRenderDispatcherExtension)this.blockEntityRenderDispatcher;
         Vec3 cameraPosition = camera.getPosition();
         BlockPos blockPos = blockEntity.getBlockPos();
         poseStack.pushPose();
         poseStack.translate(
            -((double)blockPos.getX() - cameraPosition.x()), -((double)blockPos.getY() - cameraPosition.y()), -((double)blockPos.getZ() - cameraPosition.z())
         );
         Vector3f sableCameraPosition = new Vector3f();
         SubLevelRenderData subLevelRenderData = subLevel.getRenderData();
         Vector3dc invChunkOffset = subLevel.renderPose().rotationPoint();
         Matrix4f transformation = subLevelRenderData.getTransformation(cameraPosition.x, cameraPosition.y, cameraPosition.z);
         transformation.invert(new Matrix4f()).transformPosition(sableCameraPosition.zero());
         extension.sable$setCameraPosition(
            new Vec3(
               (double)sableCameraPosition.x + invChunkOffset.x(),
               (double)sableCameraPosition.y + invChunkOffset.y(),
               (double)sableCameraPosition.z + invChunkOffset.z()
            )
         );
         poseStack.mulPose(transformation);
         this.sable$subLevelBlockEntityRenderer.renderSingleBE(blockEntity, poseStack, pt, invChunkOffset.x(), invChunkOffset.y(), invChunkOffset.z());
         poseStack.popPose();
         extension.sable$setCameraPosition(null);
      }
   }

   @Inject(
      method = {"renderLevel"},
      at = {@At(
         value = "FIELD",
         target = "Lnet/minecraft/client/renderer/LevelRenderer;globalBlockEntities:Ljava/util/Set;",
         shift = Shift.BEFORE,
         ordinal = 0
      )}
   )
   public void sable$preRenderBEs(
      DeltaTracker deltaTracker,
      boolean bl,
      Camera camera,
      GameRenderer gameRenderer,
      LightTexture lightTexture,
      Matrix4f matrix4f,
      Matrix4f matrix4f2,
      CallbackInfo ci
   ) {
      List<ClientSubLevel> subLevels = SubLevelContainer.getContainer(this.level).getAllSubLevels();
      Vec3 cameraPosition = camera.getPosition();
      SubLevelRenderDispatcher.get()
         .renderBlockEntities(
            subLevels,
            this.sable$subLevelBlockEntityRenderer,
            cameraPosition.x,
            cameraPosition.y,
            cameraPosition.z,
            deltaTracker.getGameTimeDeltaPartialTick(false)
         );
   }
}
