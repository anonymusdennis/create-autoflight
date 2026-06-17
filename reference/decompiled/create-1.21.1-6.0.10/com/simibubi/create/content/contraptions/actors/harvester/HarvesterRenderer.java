package com.simibubi.create.content.contraptions.actors.harvester;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class HarvesterRenderer extends SafeBlockEntityRenderer<HarvesterBlockEntity> {
   private static final Vec3 PIVOT = new Vec3(0.0, 6.0, 9.0);

   public HarvesterRenderer(Context context) {
   }

   protected void renderSafe(HarvesterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState blockState = be.getBlockState();
      SuperByteBuffer superBuffer = CachedBuffers.partial(AllPartialModels.HARVESTER_BLADE, blockState);
      transform(be.getLevel(), (Direction)blockState.getValue(HarvesterBlock.FACING), superBuffer, be.getAnimatedSpeed(), PIVOT);
      superBuffer.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutoutMipped()));
   }

   public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffers) {
      BlockState blockState = context.state;
      Direction facing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
      SuperByteBuffer superBuffer = CachedBuffers.partial(AllPartialModels.HARVESTER_BLADE, blockState);
      float speed = !VecHelper.isVecPointingTowards(context.relativeMotion, facing.getOpposite()) ? context.getAnimationSpeed() : 0.0F;
      if (context.contraption.stalled) {
         speed = 0.0F;
      }

      superBuffer.transform(matrices.getModel());
      transform(context.world, facing, superBuffer, speed, PIVOT);
      superBuffer.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
         .useLevelLight(context.world, matrices.getWorld())
         .renderInto(matrices.getViewProjection(), buffers.getBuffer(RenderType.cutoutMipped()));
   }

   public static void transform(Level world, Direction facing, SuperByteBuffer superBuffer, float speed, Vec3 pivot) {
      float originOffset = 0.0625F;
      Vec3 rotOffset = new Vec3(0.0, pivot.y * (double)originOffset, pivot.z * (double)originOffset);
      float time = AnimationTickHolder.getRenderTime(world) / 20.0F;
      float angle = time * speed % 360.0F;
      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)superBuffer.rotateCentered(
                  AngleHelper.rad((double)AngleHelper.horizontalAngle(facing)), Direction.UP
               ))
               .translate(rotOffset.x, rotOffset.y, rotOffset.z))
            .rotate(AngleHelper.rad((double)angle), Direction.WEST))
         .translate(-rotOffset.x, -rotOffset.y, -rotOffset.z);
   }
}
