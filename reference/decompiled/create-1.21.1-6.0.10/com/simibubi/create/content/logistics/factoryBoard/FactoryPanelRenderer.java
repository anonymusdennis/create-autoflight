package com.simibubi.create.content.logistics.factoryBoard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

public class FactoryPanelRenderer extends SmartBlockEntityRenderer<FactoryPanelBlockEntity> {
   public FactoryPanelRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(FactoryPanelBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);

      for (FactoryPanelBehaviour behaviour : be.panels.values()) {
         if (behaviour.isActive()) {
            if (behaviour.getAmount() > 0) {
               renderBulb(behaviour, partialTicks, ms, buffer, light, overlay);
            }

            for (FactoryPanelConnection connection : behaviour.targetedBy.values()) {
               renderPath(behaviour, connection, partialTicks, ms, buffer, light, overlay);
            }

            for (FactoryPanelConnection connection : behaviour.targetedByLinks.values()) {
               renderPath(behaviour, connection, partialTicks, ms, buffer, light, overlay);
            }
         }
      }
   }

   public static void renderBulb(FactoryPanelBehaviour behaviour, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = behaviour.blockEntity.getBlockState();
      float xRot = FactoryPanelBlock.getXRot(blockState) + (float) (Math.PI / 2);
      float yRot = FactoryPanelBlock.getYRot(blockState);
      float glow = behaviour.bulb.getValue(partialTicks);
      boolean missingAddress = behaviour.isMissingAddress();
      PartialModel partial = !behaviour.redstonePowered && !missingAddress ? AllPartialModels.FACTORY_PANEL_LIGHT : AllPartialModels.FACTORY_PANEL_RED_LIGHT;
      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(partial, blockState).rotateCentered(yRot, Direction.UP))
                  .rotateCentered(xRot, Direction.EAST))
               .rotateCentered((float) Math.PI, Direction.UP))
            .translate((double)behaviour.slot.xOffset * 0.5, 0.0, (double)behaviour.slot.yOffset * 0.5))
         .light(glow > 0.125F ? 15728880 : light)
         .overlay(overlay)
         .renderInto(ms, buffer.getBuffer(RenderType.translucent()));
      if (!(glow < 0.125F)) {
         glow = (float)(1.0 - 2.0 * Math.pow((double)(glow - 0.75F), 2.0));
         glow = Mth.clamp(glow, -1.0F, 1.0F);
         int color = (int)(200.0F * glow);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(partial, blockState).rotateCentered(yRot, Direction.UP))
                     .rotateCentered(xRot, Direction.EAST))
                  .rotateCentered((float) Math.PI, Direction.UP))
               .translate((double)behaviour.slot.xOffset * 0.5, 0.0, (double)behaviour.slot.yOffset * 0.5))
            .light(15728880)
            .color(color, color, color, 255)
            .overlay(overlay)
            .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
      }
   }

   public static void renderPath(
      FactoryPanelBehaviour behaviour, FactoryPanelConnection connection, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay
   ) {
      BlockState blockState = behaviour.blockEntity.getBlockState();
      List<Direction> path = connection.getPath(behaviour.getWorld(), blockState, behaviour.getPanelPosition());
      float xRot = FactoryPanelBlock.getXRot(blockState) + (float) (Math.PI / 2);
      float yRot = FactoryPanelBlock.getYRot(blockState);
      float glow = behaviour.bulb.getValue(partialTicks);
      FactoryPanelSupportBehaviour sbe = FactoryPanelBehaviour.linkAt(behaviour.getWorld(), connection);
      boolean displayLinkMode = sbe != null && sbe.blockEntity instanceof DisplayLinkBlockEntity;
      boolean redstoneLinkMode = sbe != null && sbe.blockEntity instanceof RedstoneLinkBlockEntity;
      boolean pathReversed = sbe != null && !sbe.isOutput();
      int color = 0;
      float yOffset = 0.0F;
      boolean success = connection.success;
      boolean dots = false;
      if (displayLinkMode) {
         color = 3971154;
         dots = true;
      } else if (redstoneLinkMode) {
         color = pathReversed
            ? (behaviour.count == 0 ? 8947864 : (behaviour.satisfied ? 15663104 : 5767425))
            : (behaviour.redstonePowered ? 15663104 : 5767425);
         yOffset = 0.5F;
      } else {
         color = behaviour.getIngredientStatusColor();
         yOffset = 1.0F;
         yOffset += behaviour.promisedSatisfied ? 1.0F : (behaviour.satisfied ? 0.0F : 2.0F);
         if (!behaviour.redstonePowered && !behaviour.waitingForNetwork && glow > 0.0F && !behaviour.satisfied) {
            float p = 1.0F - (1.0F - glow) * (1.0F - glow);
            color = Color.mixColors(color, success ? 15397612 : 15033675, p);
            if (!behaviour.satisfied && !behaviour.promisedSatisfied) {
               yOffset += (float)(success ? 1 : 2) * p;
            }
         }
      }

      float currentX = 0.0F;
      float currentZ = 0.0F;

      for (int i = 0; i < path.size(); i++) {
         Direction direction = path.get(i);
         if (!pathReversed) {
            currentX = (float)((double)currentX + (double)direction.getStepX() * 0.5);
            currentZ = (float)((double)currentZ + (double)direction.getStepZ() * 0.5);
         }

         boolean isArrowSegment = pathReversed ? i == path.size() - 1 : i == 0;
         PartialModel partial = (dots
               ? AllPartialModels.FACTORY_PANEL_DOTTED
               : (isArrowSegment ? AllPartialModels.FACTORY_PANEL_ARROWS : AllPartialModels.FACTORY_PANEL_LINES))
            .get(pathReversed ? direction : direction.getOpposite());
         SuperByteBuffer connectionSprite = (SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(
                           partial, blockState
                        )
                        .rotateCentered(yRot, Direction.UP))
                     .rotateCentered(xRot, Direction.EAST))
                  .rotateCentered((float) Math.PI, Direction.UP))
               .translate((double)behaviour.slot.xOffset * 0.5 + 0.25, 0.0, (double)behaviour.slot.yOffset * 0.5 + 0.25))
            .translate(currentX, (yOffset + (float)(direction.get2DDataValue() % 2) * 0.125F) / 512.0F, currentZ);
         if (!displayLinkMode
            && !redstoneLinkMode
            && !behaviour.isMissingAddress()
            && !behaviour.waitingForNetwork
            && !behaviour.satisfied
            && !behaviour.redstonePowered) {
            connectionSprite.shiftUV(AllSpriteShifts.FACTORY_PANEL_CONNECTIONS);
         }

         connectionSprite.color(color).light(light).overlay(overlay).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
         if (pathReversed) {
            currentX = (float)((double)currentX + (double)direction.getStepX() * 0.5);
            currentZ = (float)((double)currentZ + (double)direction.getStepZ() * 0.5);
         }
      }
   }
}
