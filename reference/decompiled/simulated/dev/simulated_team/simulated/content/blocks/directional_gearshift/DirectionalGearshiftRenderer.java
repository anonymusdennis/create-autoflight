package dev.simulated_team.simulated.content.blocks.directional_gearshift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class DirectionalGearshiftRenderer extends SplitShaftRenderer {
   public DirectionalGearshiftRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(SplitShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
      BlockState blockState = be.getBlockState();
      Axis axis = ((DirectionalGearshiftBlock)SimBlocks.DIRECTIONAL_GEARSHIFT.get()).getRotationAxis(blockState);
      float time = AnimationTickHolder.getRenderTime(be.getLevel());
      float angle = time * be.getSpeed() * 3.0F / 10.0F % 360.0F;
      float shaftAngle = 0.0F;
      float modifier = 0.0F;
      float offset = 0.0F;
      if (be.hasSource()
         && !(Boolean)blockState.getValue(DirectionalGearshiftBlock.LEFT_POWERED)
         && (Boolean)blockState.getValue(DirectionalGearshiftBlock.RIGHT_POWERED)) {
         shaftAngle = angle;
      }

      if (be.hasSource()
         && (Boolean)blockState.getValue(DirectionalGearshiftBlock.LEFT_POWERED)
         && !(Boolean)blockState.getValue(DirectionalGearshiftBlock.RIGHT_POWERED)) {
         modifier = be.getRotationSpeedModifier(be.getSourceFacing().getOpposite());
         offset = getRotationOffsetForPosition(be, be.getBlockPos(), axis);
      }

      angle *= modifier;
      angle += offset;
      angle = angle / 180.0F * (float) Math.PI;
      shaftAngle = shaftAngle / 180.0F * (float) Math.PI;
      Direction direction = (Direction)blockState.getValue(DirectionalGearshiftBlock.FACING);
      boolean vertical = axis.isVertical()
         || direction.getAxis().isVertical() && !(Boolean)blockState.getValue(DirectionalGearshiftBlock.AXIS_ALONG_FIRST_COORDINATE);
      VertexConsumer consumer = bufferSource.getBuffer(RenderType.solid());
      SuperByteBuffer barrel = CachedBuffers.partial(SimPartialModels.DIRECTIONAL_GEARSHIFT_CENTER, blockState);
      SuperByteBuffer barrelShaftA = CachedBuffers.partial(SimPartialModels.DIRECTIONAL_GEARSHIFT_BARREL_SHAFT, blockState);
      kineticRotationTransform(barrelShaftA, be, axis, angle, light);
      ((SuperByteBuffer)((SuperByteBuffer)barrelShaftA.center()).rotateToFace(direction)).uncenter();
      if (vertical) {
         barrelShaftA.rotateZCenteredDegrees(90.0F);
      }

      barrelShaftA.rotateZCentered((float) Math.PI);
      barrelShaftA.rotateYCentered(shaftAngle);
      barrelShaftA.light(light).renderInto(ms, consumer);
      SuperByteBuffer barrelShaftB = CachedBuffers.partial(SimPartialModels.DIRECTIONAL_GEARSHIFT_BARREL_SHAFT, blockState);
      kineticRotationTransform(barrelShaftB, be, axis, angle, light);
      ((SuperByteBuffer)((SuperByteBuffer)barrelShaftB.center()).rotateToFace(direction)).uncenter();
      if (vertical) {
         barrelShaftB.rotateZCenteredDegrees(90.0F);
      }

      barrelShaftB.rotateYCentered(shaftAngle);
      barrelShaftB.light(light).renderInto(ms, consumer);
      kineticRotationTransform(barrel, be, axis, angle, light);
      ((SuperByteBuffer)((SuperByteBuffer)barrel.center()).rotateToFace(direction)).uncenter();
      if (vertical) {
         barrel.rotateZCenteredDegrees(90.0F);
      }

      barrel.light(light).renderInto(ms, consumer);
      super.renderSafe(be, partialTicks, ms, bufferSource, light, overlay);
   }
}
