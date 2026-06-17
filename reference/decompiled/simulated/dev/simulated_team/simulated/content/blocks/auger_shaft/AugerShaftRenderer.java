package dev.simulated_team.simulated.content.blocks.auger_shaft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

public class AugerShaftRenderer extends KineticBlockEntityRenderer<AugerShaftBlockEntity> {
   public AugerShaftRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(AugerShaftBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState state = this.getRenderedBlockState(be);
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         RenderType type = this.getRenderType(be, state);
         renderRotatingBuffer(be, this.getRotatedModel(be, state), ms, buffer.getBuffer(type), light);
      }

      if (be.getBlockState().getBlock() instanceof AugerCogBlock) {
         Direction facing = Direction.get(AxisDirection.POSITIVE, (Axis)state.getValue(AugerShaftBlock.AXIS));
         VertexConsumer solid = buffer.getBuffer(RenderType.solid());

         for (int i = 0; i < 2; i++) {
            SuperByteBuffer redstone = CachedBuffers.partialFacing(
               be.flowDirection == (i == 1 ? facing.getOpposite() : facing) && be.getSpeed() != 0.0F
                  ? SimPartialModels.AUGER_REDSTONE_ON
                  : SimPartialModels.AUGER_REDSTONE_OFF,
               state,
               facing
            );
            ((PoseTransformStack)((PoseTransformStack)TransformStack.of(redstone.getTransforms()).center()).rotateToFace(facing))
               .rotate(com.mojang.math.Axis.XN.rotationDegrees((float)((facing.getAxis().isHorizontal() ? 90 : 0) + i * 180)))
               .uncenter();
            redstone.light(light).renderInto(ms, solid);
         }
      }
   }

   protected SuperByteBuffer getRotatedModel(AugerShaftBlockEntity be, BlockState state) {
      if (!(be.getBlockState().getBlock() instanceof AugerCogBlock)) {
         return super.getRotatedModel(be, state);
      } else {
         Direction facing = Direction.get(AxisDirection.POSITIVE, (Axis)state.getValue(AugerShaftBlock.AXIS));
         return CachedBuffers.partialDirectional(
            SimPartialModels.AUGER_COG,
            state,
            facing,
            () -> {
               PoseStack poseStack = new PoseStack();
               ((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).center()).rotateToFace(facing))
                  .rotate(com.mojang.math.Axis.XN.rotationDegrees(90.0F))
                  .uncenter();
               return poseStack;
            }
         );
      }
   }

   protected BlockState getRenderedBlockState(AugerShaftBlockEntity be) {
      return shaft(getRotationAxisOf(be));
   }
}
