package com.simibubi.create.content.redstone.nixieTube;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.utility.DyeHelper;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NixieTubeRenderer extends SafeBlockEntityRenderer<NixieTubeBlockEntity> {
   private static final int GLOW_VIEW_DISTANCE = 96;

   public NixieTubeRenderer(Context context) {
   }

   protected void renderSafe(NixieTubeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      ms.pushPose();
      BlockState blockState = be.getBlockState();
      DoubleFaceAttachedBlock.DoubleAttachFace face = (DoubleFaceAttachedBlock.DoubleAttachFace)blockState.getValue(NixieTubeBlock.FACE);
      float yRot = AngleHelper.horizontalAngle((Direction)blockState.getValue(NixieTubeBlock.FACING))
         - 90.0F
         + (float)(face == DoubleFaceAttachedBlock.DoubleAttachFace.WALL_REVERSED ? 180 : 0);
      float xRot = face == DoubleFaceAttachedBlock.DoubleAttachFace.WALL
         ? -90.0F
         : (face == DoubleFaceAttachedBlock.DoubleAttachFace.WALL_REVERSED ? 90.0F : 0.0F);
      PoseTransformStack msr = TransformStack.of(ms);
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)msr.center()).rotateYDegrees(yRot)).rotateZDegrees(xRot)).uncenter();
      if (be.signalState == null && be.computerSignal == null) {
         msr.center();
         float height = face == DoubleFaceAttachedBlock.DoubleAttachFace.CEILING ? 5.0F : 3.0F;
         float scale = 0.05F;
         Couple<String> s = be.getDisplayedStrings();
         DyeColor color = NixieTubeBlock.colorOf(be.getBlockState());
         RandomSource random = be.getLevel().getRandom();
         ms.pushPose();
         ms.translate(-0.25F, 0.0F, 0.0F);
         ms.scale(scale, -scale, scale);
         drawTube(ms, buffer, (String)s.getFirst(), height, color, random);
         ms.popPose();
         ms.pushPose();
         ms.translate(0.25F, 0.0F, 0.0F);
         ms.scale(scale, -scale, scale);
         drawTube(ms, buffer, (String)s.getSecond(), height, color, random);
         ms.popPose();
         ms.popPose();
      } else {
         this.renderAsSignal(be, partialTicks, ms, buffer, light, overlay);
         ms.popPose();
      }
   }

   public static void drawTube(PoseStack ms, MultiBufferSource buffer, String c, float height, DyeColor color, RandomSource random) {
      Font fontRenderer = Minecraft.getInstance().font;
      float charWidth = (float)fontRenderer.width(c);
      float shadowOffset = 0.5F;
      float flicker = random.nextFloat();
      Couple<Integer> couple = DyeHelper.getDyeColors(color);
      int brightColor = (Integer)couple.getFirst();
      int darkColor = (Integer)couple.getSecond();
      int flickeringBrightColor = Color.mixColors(brightColor, darkColor, flicker / 4.0F);
      ms.pushPose();
      ms.translate((charWidth - shadowOffset) / -2.0F, -height, 0.0F);
      drawInWorldString(ms, buffer, c, flickeringBrightColor);
      ms.pushPose();
      ms.translate(shadowOffset, shadowOffset, -0.0625F);
      drawInWorldString(ms, buffer, c, darkColor);
      ms.popPose();
      ms.popPose();
      ms.pushPose();
      ms.scale(-1.0F, 1.0F, 1.0F);
      ms.translate((charWidth - shadowOffset) / -2.0F, -height, 0.0F);
      drawInWorldString(ms, buffer, c, darkColor);
      ms.pushPose();
      ms.translate(-shadowOffset, shadowOffset, -0.0625F);
      drawInWorldString(ms, buffer, c, Color.mixColors(darkColor, 0, 0.35F));
      ms.popPose();
      ms.popPose();
   }

   public static void drawInWorldString(PoseStack ms, MultiBufferSource buffer, String c, int color) {
      Font fontRenderer = Minecraft.getInstance().font;
      fontRenderer.drawInBatch(c, 0.0F, 0.0F, color, false, ms.last().pose(), buffer, DisplayMode.NORMAL, 0, 15728880);
      if (buffer instanceof BufferSource) {
         BakedGlyph texturedglyph = fontRenderer.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
         ((BufferSource)buffer).endBatch(texturedglyph.renderType(DisplayMode.NORMAL));
      }
   }

   private void renderAsSignal(NixieTubeBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = be.getBlockState();
      Direction facing = NixieTubeBlock.getFacing(blockState);
      Vec3 observerVec = Minecraft.getInstance().cameraEntity.getEyePosition(partialTicks);
      PoseTransformStack msr = TransformStack.of(ms);
      if (facing == Direction.DOWN) {
         ((PoseTransformStack)((PoseTransformStack)msr.center()).rotateZDegrees(180.0F)).uncenter();
      }

      boolean invertTubes = facing == Direction.DOWN || blockState.getValue(NixieTubeBlock.FACE) == DoubleFaceAttachedBlock.DoubleAttachFace.WALL_REVERSED;
      CachedBuffers.partial(AllPartialModels.SIGNAL_PANEL, blockState).light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      ms.pushPose();
      ms.translate(0.5F, 0.46875F, 0.5F);
      float renderTime = AnimationTickHolder.getRenderTime(be.getLevel());
      Vec3 lampVec = Vec3.atCenterOf(be.getBlockPos());
      Vec3 diff = lampVec.subtract(observerVec);
      if (be.signalState != null) {
         for (boolean first : Iterate.trueAndFalse) {
            if ((!first || be.signalState.isRedLight(renderTime))
               && (first || be.signalState.isGreenLight(renderTime) || be.signalState.isYellowLight(renderTime))) {
               boolean flip = first == invertTubes;
               boolean yellow = be.signalState.isYellowLight(renderTime);
               ms.pushPose();
               ms.translate(flip ? 0.25F : -0.25F, 0.0F, 0.0F);
               if (diff.lengthSqr() < 9216.0) {
                  boolean vert = first ^ facing.getAxis().isHorizontal();
                  float longSide = yellow ? 1.0F : 4.0F;
                  float longSideGlow = yellow ? 2.0F : 5.125F;
                  ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.SIGNAL_WHITE_CUBE, blockState)
                        .light(15728880)
                        .disableDiffuse()
                        .scale(vert ? longSide : 1.0F, vert ? 1.0F : longSide, 1.0F))
                     .renderInto(ms, buffer.getBuffer(RenderType.translucent()));
                  ((SuperByteBuffer)CachedBuffers.partial(
                           first ? AllPartialModels.SIGNAL_RED_GLOW : (yellow ? AllPartialModels.SIGNAL_YELLOW_GLOW : AllPartialModels.SIGNAL_WHITE_GLOW),
                           blockState
                        )
                        .light(15728880)
                        .disableDiffuse()
                        .scale(vert ? longSideGlow : 2.0F, vert ? 2.0F : longSideGlow, 2.0F))
                     .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
               }

               ((SuperByteBuffer)CachedBuffers.partial(
                        first ? AllPartialModels.SIGNAL_RED : (yellow ? AllPartialModels.SIGNAL_YELLOW : AllPartialModels.SIGNAL_WHITE), blockState
                     )
                     .light(15728880)
                     .disableDiffuse()
                     .scale(1.0625F))
                  .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
               ms.popPose();
            }
         }
      } else if (be.computerSignal != null) {
         for (boolean firstx : Iterate.trueAndFalse) {
            NixieTubeBlockEntity.ComputerSignal.TubeDisplay tubeDisplay = firstx ? be.computerSignal.first : be.computerSignal.second;
            if (tubeDisplay.blinkPeriod != 0
               && (tubeDisplay.blinkPeriod <= 1 || !(renderTime % (float)tubeDisplay.blinkPeriod < (float)tubeDisplay.blinkOffTime))) {
               boolean flip = firstx == invertTubes;
               ms.pushPose();
               ms.translate(flip ? 0.25F : -0.25F, 0.0F, 0.0F);
               if (diff.lengthSqr() < 9216.0) {
                  boolean horiz = facing.getAxis().isHorizontal();
                  float width = horiz ? (float)tubeDisplay.glowWidth : (float)tubeDisplay.glowHeight;
                  float height = horiz ? (float)tubeDisplay.glowHeight : (float)tubeDisplay.glowWidth;
                  ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.SIGNAL_COMPUTER_WHITE_CUBE, blockState)
                        .light(15728880)
                        .disableDiffuse()
                        .scale(width, height, 1.0F))
                     .renderInto(ms, buffer.getBuffer(RenderType.translucent()));
                  ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.SIGNAL_COMPUTER_WHITE_GLOW, blockState)
                        .light(15728880)
                        .color(
                           Math.min((tubeDisplay.r & 255) * 6 + 256 >> 3, 255),
                           Math.min((tubeDisplay.g & 255) * 6 + 256 >> 3, 255),
                           Math.min((tubeDisplay.b & 255) * 6 + 256 >> 3, 255),
                           255
                        )
                        .disableDiffuse()
                        .scale(width + 1.125F, height + 1.125F, 2.0F))
                     .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
               }

               ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.SIGNAL_COMPUTER_WHITE_BASE, blockState)
                     .light(15728880)
                     .color(12, 12, 12, 255)
                     .disableDiffuse()
                     .scale(1.078125F))
                  .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
               ((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.SIGNAL_COMPUTER_WHITE, blockState)
                     .light(15728880)
                     .color(tubeDisplay.r, tubeDisplay.g, tubeDisplay.b, 255)
                     .disableDiffuse()
                     .scale(1.0625F))
                  .renderInto(ms, buffer.getBuffer(RenderTypes.additive()));
               ms.popPose();
            }
         }
      }

      ms.popPose();
   }

   public int getViewDistance() {
      return 128;
   }
}
