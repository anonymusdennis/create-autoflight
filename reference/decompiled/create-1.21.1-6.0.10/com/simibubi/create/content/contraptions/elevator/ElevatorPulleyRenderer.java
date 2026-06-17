package com.simibubi.create.content.contraptions.elevator;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.pulley.AbstractPulleyRenderer;
import com.simibubi.create.content.contraptions.pulley.PulleyRenderer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ElevatorPulleyRenderer extends KineticBlockEntityRenderer<ElevatorPulleyBlockEntity> {
   public ElevatorPulleyRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(ElevatorPulleyBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      float offset = PulleyRenderer.getBlockEntityOffset(partialTicks, be);
      boolean running = PulleyRenderer.isPulleyRunning(be);
      SpriteShiftEntry beltShift = AllSpriteShifts.ELEVATOR_BELT;
      SpriteShiftEntry coilShift = AllSpriteShifts.ELEVATOR_COIL;
      VertexConsumer vb = buffer.getBuffer(RenderType.solid());
      Level world = be.getLevel();
      BlockState blockState = be.getBlockState();
      BlockPos pos = be.getBlockPos();
      float blockStateAngle = 180.0F + AngleHelper.horizontalAngle((Direction)blockState.getValue(ElevatorPulleyBlock.HORIZONTAL_FACING));
      SuperByteBuffer magnet = CachedBuffers.partial(AllPartialModels.ELEVATOR_MAGNET, blockState);
      if (running || offset == 0.0F) {
         AbstractPulleyRenderer.renderAt(
            world, (SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)magnet.center()).rotateYDegrees(blockStateAngle)).uncenter(), offset, pos, ms, vb
         );
      }

      SuperByteBuffer rotatedCoil = this.getRotatedCoil(be);
      if (offset == 0.0F) {
         rotatedCoil.light(light).renderInto(ms, vb);
      } else {
         AbstractPulleyRenderer.scrollCoil(rotatedCoil, coilShift, offset, 2.0F).light(light).renderInto(ms, vb);
         float spriteSize = beltShift.getTarget().getV1() - beltShift.getTarget().getV0();
         double beltScroll = (-((double)offset + 0.5) - Math.floor(-((double)offset + 0.5))) / 2.0;
         SuperByteBuffer halfRope = CachedBuffers.partial(AllPartialModels.ELEVATOR_BELT_HALF, blockState);
         SuperByteBuffer rope = CachedBuffers.partial(AllPartialModels.ELEVATOR_BELT, blockState);
         float f = offset % 1.0F;
         if (f < 0.25F || f > 0.75F) {
            ((SuperByteBuffer)((SuperByteBuffer)halfRope.center()).rotateYDegrees(blockStateAngle)).uncenter();
            AbstractPulleyRenderer.renderAt(world, halfRope.shiftUVScrolling(beltShift, (float)beltScroll * spriteSize), f > 0.75F ? f - 1.0F : f, pos, ms, vb);
         }

         if (running) {
            for (int i = 0; (float)i < offset - 0.25F; i++) {
               ((SuperByteBuffer)((SuperByteBuffer)rope.center()).rotateYDegrees(blockStateAngle)).uncenter();
               AbstractPulleyRenderer.renderAt(world, rope.shiftUVScrolling(beltShift, (float)beltScroll * spriteSize), offset - (float)i, pos, ms, vb);
            }
         }
      }
   }

   protected BlockState getRenderedBlockState(ElevatorPulleyBlockEntity be) {
      return shaft(getRotationAxisOf(be));
   }

   protected SuperByteBuffer getRotatedCoil(KineticBlockEntity be) {
      BlockState blockState = be.getBlockState();
      return CachedBuffers.partialFacing(AllPartialModels.ELEVATOR_COIL, blockState, (Direction)blockState.getValue(ElevatorPulleyBlock.HORIZONTAL_FACING));
   }

   public boolean shouldRenderOffScreen(ElevatorPulleyBlockEntity p_188185_1_) {
      return true;
   }
}
