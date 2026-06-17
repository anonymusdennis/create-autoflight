package com.simibubi.create.foundation.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class BlockEntityRenderHelper {
   public static void renderBlockEntities(
      List<BlockEntity> blockEntities,
      BitSet shouldRenderBEs,
      BitSet erroredBEsOut,
      @Nullable VirtualRenderWorld renderLevel,
      Level realLevel,
      PoseStack ms,
      @Nullable Matrix4f lightTransform,
      MultiBufferSource buffer,
      float pt
   ) {
      for (int i = shouldRenderBEs.nextSetBit(0); i >= 0 && i < blockEntities.size(); i = shouldRenderBEs.nextSetBit(i + 1)) {
         BlockEntity blockEntity = blockEntities.get(i);
         if (!VisualizationManager.supportsVisualization(realLevel) || !VisualizationHelper.skipVanillaRender(blockEntity)) {
            BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (renderer == null) {
               erroredBEsOut.set(i);
            } else {
               BlockPos pos = blockEntity.getBlockPos();
               ms.pushPose();
               TransformStack.of(ms).translate(pos);

               try {
                  int realLevelLight = LevelRenderer.getLightColor(realLevel, getLightPos(lightTransform, pos));
                  int light;
                  if (renderLevel != null) {
                     renderLevel.setExternalLight(realLevelLight);
                     light = LevelRenderer.getLightColor(renderLevel, pos);
                  } else {
                     light = realLevelLight;
                  }

                  renderer.render(blockEntity, pt, ms, buffer, light, OverlayTexture.NO_OVERLAY);
               } catch (Exception var15) {
                  erroredBEsOut.set(i);
                  String message = "BlockEntity " + RegisteredObjectsHelper.getKeyOrThrow(blockEntity.getType()) + " could not be rendered virtually.";
                  if ((Boolean)AllConfigs.client().explainRenderErrors.get()) {
                     Create.LOGGER.error(message, var15);
                  } else {
                     Create.LOGGER.error(message);
                  }
               }

               ms.popPose();
            }
         }
      }

      if (renderLevel != null) {
         renderLevel.resetExternalLight();
      }
   }

   private static BlockPos getLightPos(@org.jetbrains.annotations.Nullable Matrix4f lightTransform, BlockPos contraptionPos) {
      if (lightTransform != null) {
         Vector4f lightVec = new Vector4f((float)contraptionPos.getX() + 0.5F, (float)contraptionPos.getY() + 0.5F, (float)contraptionPos.getZ() + 0.5F, 1.0F);
         lightVec.mul(lightTransform);
         return BlockPos.containing((double)lightVec.x(), (double)lightVec.y(), (double)lightVec.z());
      } else {
         return contraptionPos;
      }
   }
}
