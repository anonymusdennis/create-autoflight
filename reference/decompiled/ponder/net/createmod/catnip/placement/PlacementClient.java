package net.createmod.catnip.placement;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.config.CClient;
import net.createmod.ponder.enums.PonderConfig;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class PlacementClient {
   static final LerpedFloat angle = LerpedFloat.angular().chase(0.0, 0.25, LerpedFloat.Chaser.EXP);
   @Nullable
   static BlockPos target = null;
   @Nullable
   static BlockPos lastTarget = null;
   static int animationTick = 0;

   public static void tick() {
      setTarget(null);
      checkHelpers();
      if (target == null) {
         if (animationTick > 0) {
            animationTick = Math.max(animationTick - 2, 0);
         }
      } else {
         if (animationTick < 10) {
            animationTick++;
         }
      }
   }

   private static void checkHelpers() {
      Minecraft mc = Minecraft.getInstance();
      ClientLevel world = mc.level;
      if (world != null) {
         if (mc.hitResult instanceof BlockHitResult ray) {
            if (mc.player != null) {
               if (!mc.player.isShiftKeyDown()) {
                  for (InteractionHand hand : InteractionHand.values()) {
                     ItemStack heldItem = mc.player.getItemInHand(hand);
                     List<IPlacementHelper> filteredForHeldItem = new ArrayList<>();

                     for (IPlacementHelper helper : PlacementHelpers.getHelpersView()) {
                        if (helper.matchesItem(heldItem)) {
                           filteredForHeldItem.add(helper);
                        }
                     }

                     if (!filteredForHeldItem.isEmpty()) {
                        BlockPos pos = ray.getBlockPos();
                        BlockState state = world.getBlockState(pos);
                        List<IPlacementHelper> filteredForState = new ArrayList<>();

                        for (IPlacementHelper helperx : filteredForHeldItem) {
                           if (helperx.matchesState(state)) {
                              filteredForState.add(helperx);
                           }
                        }

                        if (!filteredForState.isEmpty()) {
                           boolean atLeastOneMatch = false;

                           for (IPlacementHelper h : filteredForState) {
                              PlacementOffset offset = h.getOffset(mc.player, world, state, pos, ray, heldItem);
                              if (offset.isSuccessful()) {
                                 h.renderAt(pos, state, ray, offset);
                                 setTarget(offset.getBlockPos());
                                 atLeastOneMatch = true;
                                 break;
                              }
                           }

                           if (atLeastOneMatch) {
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   static void setTarget(@Nullable BlockPos target) {
      PlacementClient.target = target;
      if (target != null) {
         if (lastTarget == null) {
            lastTarget = target;
         } else {
            if (!lastTarget.equals(target)) {
               lastTarget = target;
            }
         }
      }
   }

   public static void onRenderCrosshairOverlay(GuiGraphics graphics, float partialTicks) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null && animationTick > 0) {
         float screenY = (float)graphics.guiHeight() / 2.0F;
         float screenX = (float)graphics.guiWidth() / 2.0F;
         float progress = getCurrentAlpha();
         drawDirectionIndicator(graphics, partialTicks, screenX, screenY, progress);
      }
   }

   public static float getCurrentAlpha() {
      return Math.min((float)animationTick / 10.0F, 1.0F);
   }

   private static void drawDirectionIndicator(GuiGraphics graphics, float partialTicks, float centerX, float centerY, float progress) {
      float r = 0.8F;
      float g = 0.8F;
      float b = 0.8F;
      float a = progress * progress;
      Vec3 projTarget = VecHelper.projectToPlayerView(VecHelper.getCenterOf(lastTarget), partialTicks);
      Vec3 target = new Vec3(projTarget.x, projTarget.y, 0.0);
      if (projTarget.z > 0.0) {
         target = target.reverse();
      }

      Vec3 norm = target.normalize();
      Vec3 ref = new Vec3(0.0, 1.0, 0.0);
      float targetAngle = AngleHelper.deg(-Math.acos(norm.dot(ref)));
      if (norm.x < 0.0) {
         targetAngle = 360.0F - targetAngle;
      }

      if (animationTick < 10) {
         angle.setValue((double)targetAngle);
      }

      angle.chase((double)targetAngle, 0.25, LerpedFloat.Chaser.EXP);
      angle.tickChaser();
      float snapSize = 22.5F;
      float snappedAngle = snapSize * (float)Math.round(angle.getValue(0.0F) / snapSize) % 360.0F;
      float length = 10.0F;
      CClient.PlacementIndicatorSetting mode = PonderConfig.client().placementIndicator.get();
      PoseStack poseStack = graphics.pose();
      if (mode == CClient.PlacementIndicatorSetting.TRIANGLE) {
         fadedArrow(poseStack, centerX, centerY, r, g, b, a, length, snappedAngle);
      } else if (mode == CClient.PlacementIndicatorSetting.TEXTURE) {
         textured(poseStack, centerX, centerY, a, snappedAngle);
      }
   }

   private static void fadedArrow(PoseStack ms, float centerX, float centerY, float r, float g, float b, float a, float length, float snappedAngle) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      ms.pushPose();
      ms.translate(centerX, centerY, 5.0F);
      ms.mulPose(Axis.ZP.rotationDegrees(angle.getValue(0.0F)));
      double scale = PonderConfig.client().indicatorScale.get();
      ms.scale((float)scale, (float)scale, 1.0F);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f mat = ms.last().pose();
      bufferbuilder.addVertex(mat, 0.0F, -(10.0F + length), 0.0F).setColor(r, g, b, a);
      bufferbuilder.addVertex(mat, -9.0F, -3.0F, 0.0F).setColor(r, g, b, 0.0F);
      bufferbuilder.addVertex(mat, -6.0F, -6.0F, 0.0F).setColor(r, g, b, 0.0F);
      bufferbuilder.addVertex(mat, -3.0F, -8.0F, 0.0F).setColor(r, g, b, 0.0F);
      bufferbuilder.addVertex(mat, 0.0F, -8.5F, 0.0F).setColor(r, g, b, 0.0F);
      bufferbuilder.addVertex(mat, 3.0F, -8.0F, 0.0F).setColor(r, g, b, 0.0F);
      bufferbuilder.addVertex(mat, 6.0F, -6.0F, 0.0F).setColor(r, g, b, 0.0F);
      bufferbuilder.addVertex(mat, 9.0F, -3.0F, 0.0F).setColor(r, g, b, 0.0F);
      BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
      RenderSystem.disableBlend();
      ms.popPose();
   }

   public static void textured(PoseStack ms, float centerX, float centerY, float alpha, float snappedAngle) {
      PonderGuiTextures.PLACEMENT_INDICATOR_SHEET.bind();
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      ms.pushPose();
      ms.translate(centerX, centerY, 50.0F);
      float scale = PonderConfig.client().indicatorScale.get().floatValue() * 0.75F;
      ms.scale(scale, scale, 1.0F);
      ms.scale(12.0F, 12.0F, 1.0F);
      float index = snappedAngle / 22.5F;
      float tex_size = 0.0625F;
      float tx = 0.0F;
      float ty = index * tex_size;
      float tw = 1.0F;
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder buffer = tesselator.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      Matrix4f mat = ms.last().pose();
      buffer.addVertex(mat, -1.0F, -1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(tx, ty);
      buffer.addVertex(mat, -1.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(tx, ty + tex_size);
      buffer.addVertex(mat, 1.0F, 1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(tx + tw, ty + tex_size);
      buffer.addVertex(mat, 1.0F, -1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, alpha).setUv(tx + tw, ty);
      BufferUploader.drawWithShader(buffer.buildOrThrow());
      RenderSystem.disableBlend();
      ms.popPose();
   }
}
