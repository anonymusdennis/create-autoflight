package com.simibubi.create.content.contraptions.actors.contraptionControls;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ContraptionControlsRenderer extends SmartBlockEntityRenderer<ContraptionControlsBlockEntity> {
   public ContraptionControlsRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(ContraptionControlsBlockEntity blockEntity, float pt, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = blockEntity.getBlockState();
      Direction facing = ((Direction)blockState.getValue(ContraptionControlsBlock.FACING)).getOpposite();
      Vec3 buttonMovementAxis = VecHelper.rotate(new Vec3(0.0, 1.0, -0.325), (double)AngleHelper.horizontalAngle(facing), Axis.Y);
      Vec3 buttonMovement = buttonMovementAxis.scale((double)(-0.07F + -0.041666668F * blockEntity.button.getValue(pt)));
      Vec3 buttonOffset = buttonMovementAxis.scale(0.07F);
      ms.pushPose();
      ms.translate(buttonMovement.x, buttonMovement.y, buttonMovement.z);
      super.renderSafe(blockEntity, pt, ms, buffer, light, overlay);
      ms.translate(buttonOffset.x, buttonOffset.y, buttonOffset.z);
      VertexConsumer vc = buffer.getBuffer(RenderType.solid());
      CachedBuffers.partialFacing(AllPartialModels.CONTRAPTION_CONTROLS_BUTTON, blockState, facing).light(light).renderInto(ms, vc);
      ms.popPose();
      int i = (int)blockEntity.indicator.getValue(pt) / 45 % 8 + 8;
      CachedBuffers.partialFacing(AllPartialModels.CONTRAPTION_CONTROLS_INDICATOR.get(i % 8), blockState, facing).light(light).renderInto(ms, vc);
   }

   public static void renderInContraption(MovementContext ctx, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
      if (ctx.temporaryData instanceof ContraptionControlsMovement.ElevatorFloorSelection efs) {
         if (AllBlocks.CONTRAPTION_CONTROLS.has(ctx.state)) {
            Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
            float playerDistance = (float)(ctx.position != null && cameraEntity != null ? ctx.position.distanceToSqr(cameraEntity.getEyePosition()) : 0.0);
            float flicker = renderWorld.random.nextFloat();
            Couple<Integer> couple = DyeHelper.getDyeColors(efs.targetYEqualsSelection ? DyeColor.WHITE : DyeColor.ORANGE);
            int brightColor = (Integer)couple.getFirst();
            int darkColor = (Integer)couple.getSecond();
            int flickeringBrightColor = Color.mixColors(brightColor, darkColor, flicker / 4.0F);
            Font fontRenderer = Minecraft.getInstance().font;
            float shadowOffset = 0.5F;
            String text = efs.currentShortName;
            String description = efs.currentLongName;
            PoseStack ms = matrices.getViewProjection();
            PoseTransformStack msr = TransformStack.of(ms);
            float buttondepth = 0.0F;
            if (ctx.contraption.getBlockEntityClientSide(ctx.localPos) instanceof ContraptionControlsBlockEntity cbe) {
               buttondepth = -0.041666668F * cbe.button.getValue(AnimationTickHolder.getPartialTicks(renderWorld));
            }

            ms.pushPose();
            msr.translate(ctx.localPos);
            ms.translate(0.0F, buttondepth, 0.0F);
            VertexConsumer vc = buffer.getBuffer(RenderType.solid());
            CachedBuffers.partialFacing(
                  AllPartialModels.CONTRAPTION_CONTROLS_BUTTON, ctx.state, ((Direction)ctx.state.getValue(ContraptionControlsBlock.FACING)).getOpposite()
               )
               .light(LevelRenderer.getLightColor(renderWorld, ctx.localPos))
               .useLevelLight(ctx.world, matrices.getWorld())
               .renderInto(ms, vc);
            ms.popPose();
            ms.pushPose();
            msr.translate(ctx.localPos);
            msr.rotateCentered(
               AngleHelper.rad((double)AngleHelper.horizontalAngle((Direction)ctx.state.getValue(ContraptionControlsBlock.FACING))), Direction.UP
            );
            ms.translate(0.4F, 1.125F, 0.5F);
            msr.rotate(AngleHelper.rad(67.5), Direction.WEST);
            if (!text.isBlank() && playerDistance < 100.0F) {
               int actualWidth = fontRenderer.width(text);
               int width = Math.max(actualWidth, 12);
               float scale = 1.0F / (5.0F * ((float)width - 0.5F));
               float heightCentering = ((float)width - 8.0F) / 2.0F;
               ms.pushPose();
               ms.translate(0.0F, 0.15F, buttondepth - 0.25F);
               ms.scale(scale, -scale, scale);
               ms.translate((float)Math.max(0, width - actualWidth) / 2.0F, heightCentering, 0.0F);
               NixieTubeRenderer.drawInWorldString(ms, buffer, text, flickeringBrightColor);
               ms.translate(shadowOffset, shadowOffset, -0.0625F);
               NixieTubeRenderer.drawInWorldString(ms, buffer, text, Color.mixColors(darkColor, 0, 0.35F));
               ms.popPose();
            }

            if (!description.isBlank() && playerDistance < 20.0F) {
               int actualWidth = fontRenderer.width(description);
               int width = Math.max(actualWidth, 55);
               float scale = 1.0F / (3.0F * ((float)width - 0.5F));
               float heightCentering = ((float)width - 8.0F) / 2.0F;
               ms.pushPose();
               ms.translate(-0.0635F, 0.06F, buttondepth - 0.25F);
               ms.scale(scale, -scale, scale);
               ms.translate((float)Math.max(0, width - actualWidth) / 2.0F, heightCentering, 0.0F);
               NixieTubeRenderer.drawInWorldString(ms, buffer, description, flickeringBrightColor);
               ms.popPose();
            }

            ms.popPose();
         }
      }
   }
}
