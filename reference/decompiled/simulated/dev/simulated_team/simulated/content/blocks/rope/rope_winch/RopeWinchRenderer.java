package dev.simulated_team.simulated.content.blocks.rope.rope_winch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.contraptions.pulley.AbstractPulleyRenderer;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimSpriteShifts;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RopeWinchRenderer extends SafeBlockEntityRenderer<RopeWinchBlockEntity> {
   public RopeWinchRenderer(Context context) {
   }

   private static SuperByteBuffer transform(SuperByteBuffer buffer, BlockState state, boolean axisDirectionMatters) {
      Direction facing = (Direction)state.getValue(DirectionalKineticBlock.FACING);
      float zRotLast = axisDirectionMatters && state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Axis.Z
         ? 90.0F
         : 0.0F;
      float yRot = AngleHelper.horizontalAngle(facing)
         + (!state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) && facing.getAxis() == Axis.Y ? 90.0F : 0.0F);
      float zRot = facing == Direction.UP ? 270.0F : (facing == Direction.DOWN ? 90.0F : 0.0F);
      buffer.rotateCentered((float)((double)(zRot / 180.0F) * Math.PI), Direction.SOUTH);
      buffer.rotateCentered((float)((double)(yRot / 180.0F) * Math.PI), Direction.UP);
      buffer.rotateCentered((float)((double)(zRotLast / 180.0F) * Math.PI), Direction.SOUTH);
      return buffer;
   }

   public boolean shouldRenderOffScreen(RopeWinchBlockEntity be) {
      return true;
   }

   public boolean shouldRender(RopeWinchBlockEntity pBlockEntity, Vec3 pCameraPos) {
      return true;
   }

   protected void renderSafe(RopeWinchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
      this.renderComponents(be, partialTicks, ms, buffer, light, overlay);
   }

   protected void renderComponents(RopeWinchBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      ms.pushPose();
      VertexConsumer vb = buffer.getBuffer(RenderType.solid());
      BlockState state = be.getBlockState();
      SuperByteBuffer shaft = CachedBuffers.partial(SimPartialModels.ROPE_WINCH_SHAFT, state);
      SuperByteBuffer ropeCoil = CachedBuffers.partial(SimPartialModels.ROPE_WINCH_ROPE_COIL, state);
      Axis axis = KineticBlockEntityRenderer.getRotationAxisOf(be);
      float angle = KineticBlockEntityRenderer.getAngleForBe(be, be.getBlockPos(), axis);
      KineticBlockEntityRenderer.kineticRotationTransform(shaft, be, axis, angle, light);
      transform(shaft, state, true).renderInto(ms, vb);
      if (be.getRopeHolder().isAttached() || be.isVirtual() && be.getRopeHolder().renderAttached) {
         ropeCoil.light(light);
         Direction facing = (Direction)state.getValue(DirectionalKineticBlock.FACING);
         float speed;
         if (facing == Direction.DOWN) {
            speed = facing.getAxisDirection() == AxisDirection.NEGATIVE ? 1.0F : -1.0F;
         } else {
            speed = facing.getAxisDirection() == AxisDirection.NEGATIVE == state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
               ? 1.0F
               : -1.0F;
         }

         AbstractPulleyRenderer.scrollCoil(ropeCoil, this.getCoilShift(), be.clientAngle.getValue(partialTicks), speed);
         transform(ropeCoil, state, true).renderInto(ms, vb);
      }

      ms.popPose();
      RopeStrandRenderer.render(be, be.getRopeHolder(), partialTicks, ms, buffer);
   }

   protected SpriteShiftEntry getCoilShift() {
      return SimSpriteShifts.ROPE_WINCH_COIL;
   }
}
