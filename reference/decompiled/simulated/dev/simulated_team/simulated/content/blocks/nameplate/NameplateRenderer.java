package dev.simulated_team.simulated.content.blocks.nameplate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.simulated_team.simulated.data.SimLang;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class NameplateRenderer extends SafeBlockEntityRenderer<NameplateBlockEntity> {
   private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);
   private final Context context;

   public NameplateRenderer(Context context) {
      this.context = context;
   }

   public void renderSafe(NameplateBlockEntity be, float pPartialTick, PoseStack ps, MultiBufferSource pBuffer, int packedLight, int pPackedOverlay) {
      Font font = this.context.getFont();
      BlockState state = be.getBlockState();
      Direction facing = (Direction)state.getValue(NameplateBlock.FACING);
      NameplateBlock.Position pos = (NameplateBlock.Position)state.getValue(NameplateBlock.POSITION);
      if (pos == NameplateBlock.Position.LEFT || pos == NameplateBlock.Position.SINGLE) {
         ps.pushPose();
         ps.translate(0.5, 0.5, 0.5);
         ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180.0F));
         ps.translate(-0.5, -0.5, -0.5);
         ps.translate(1.0, 1.0, 1.0);
         ps.translate(0.0, 0.0, -0.253125);
         int pixelsTall = be.glowing ? 5 : 6;
         int pixelsLeft = 3;
         ps.translate(-0.1875, -(16.0 - (double)pixelsTall) / 16.0 / 2.0, 0.0);
         ps.scale((float)((double)pixelsTall / 16.0), (float)((double)pixelsTall / 16.0), (float)((double)pixelsTall / 16.0));
         ps.scale(0.14285715F, 0.14285715F, 0.14285715F);
         ps.mulPose(Axis.ZP.rotationDegrees(180.0F));
         int availableSpace = (be.getControllerWidth() * 16 - 6) * 7 / pixelsTall + 1;
         String trimmed = font.plainSubstrByWidth(be.getName(), availableSpace);
         int width = font.width(trimmed);
         double centerPixels = (double)(availableSpace - 1) / 2.0 - (double)width / 2.0;
         ps.translate(centerPixels, 0.0, 0.0);
         MutableComponent textComponent = SimLang.text(trimmed).component();
         List<FormattedCharSequence> sequences = font.split(textComponent, width);
         int textColor;
         boolean glowing;
         if (be.glowing) {
            textColor = be.getTextColor().getTextColor();
            glowing = isOutlineVisible(be.getBlockPos(), textColor);
            packedLight = 15728880;
         } else {
            textColor = be.getDarkColor(be.getTextColor());
            glowing = false;
         }

         for (FormattedCharSequence sequence : sequences) {
            if (glowing) {
               font.drawInBatch8xOutline(sequence, 0.0F, 0.0F, textColor, be.getDarkColor(be.getTextColor()), ps.last().pose(), pBuffer, packedLight);
            } else {
               font.drawInBatch(sequence, 0.0F, 0.0F, textColor, false, ps.last().pose(), pBuffer, DisplayMode.NORMAL, 0, packedLight);
            }
         }

         ps.popPose();
      }
   }

   private static boolean isOutlineVisible(BlockPos blockPos, int i) {
      if (i == DyeColor.BLACK.getTextColor()) {
         return true;
      } else {
         Minecraft minecraft = Minecraft.getInstance();
         LocalPlayer localPlayer = minecraft.player;
         if (localPlayer != null && minecraft.options.getCameraType().isFirstPerson() && localPlayer.isScoping()) {
            return true;
         } else {
            Entity entity = minecraft.getCameraEntity();
            return entity != null && entity.distanceToSqr(Vec3.atCenterOf(blockPos)) < (double)OUTLINE_RENDER_DISTANCE;
         }
      }
   }
}
