package com.simibubi.create.content.contraptions.gantry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

public class GantryCarriageRenderer extends KineticBlockEntityRenderer<GantryCarriageBlockEntity> {
   public GantryCarriageRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(GantryCarriageBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState state = be.getBlockState();
         Direction facing = (Direction)state.getValue(GantryCarriageBlock.FACING);
         Boolean alongFirst = (Boolean)state.getValue(GantryCarriageBlock.AXIS_ALONG_FIRST_COORDINATE);
         Axis rotationAxis = getRotationAxisOf(be);
         BlockPos visualPos = facing.getAxisDirection() == AxisDirection.POSITIVE ? be.getBlockPos() : be.getBlockPos().relative(facing.getOpposite());
         float angleForBE = getAngleForBE(be, visualPos, rotationAxis);
         Axis gantryAxis = Axis.X;

         for (Axis axis : Iterate.axes) {
            if (axis != rotationAxis && axis != facing.getAxis()) {
               gantryAxis = axis;
            }
         }

         if (gantryAxis == Axis.X && facing == Direction.UP) {
            angleForBE *= -1.0F;
         }

         if (gantryAxis == Axis.Y && (facing == Direction.NORTH || facing == Direction.EAST)) {
            angleForBE *= -1.0F;
         }

         SuperByteBuffer cogs = CachedBuffers.partial(AllPartialModels.GANTRY_COGS, state);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)cogs.center())
                              .rotateYDegrees(AngleHelper.horizontalAngle(facing)))
                           .rotateXDegrees(facing == Direction.UP ? 0.0F : (facing == Direction.DOWN ? 180.0F : 90.0F)))
                        .rotateYDegrees(alongFirst ^ facing.getAxis() == Axis.X ? 0.0F : 90.0F))
                     .translate(0.0F, -0.5625F, 0.0F))
                  .rotateXDegrees(-angleForBE))
               .translate(0.0F, 0.5625F, 0.0F))
            .uncenter();
         cogs.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }

   public static float getAngleForBE(KineticBlockEntity be, BlockPos pos, Axis axis) {
      float time = AnimationTickHolder.getRenderTime(be.getLevel());
      float offset = getRotationOffsetForPosition(be, pos, axis);
      return (time * be.getSpeed() * 3.0F / 20.0F + offset) % 360.0F;
   }

   protected BlockState getRenderedBlockState(GantryCarriageBlockEntity be) {
      return shaft(getRotationAxisOf(be));
   }
}
