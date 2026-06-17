package com.simibubi.create.content.contraptions.bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class BearingRenderer<T extends KineticBlockEntity & IBearingBlockEntity> extends KineticBlockEntityRenderer<T> {
   public BearingRenderer(Context context) {
      super(context);
   }

   @Override
   protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         Direction facing = (Direction)be.getBlockState().getValue(BlockStateProperties.FACING);
         PartialModel top = be.isWoodenTop() ? AllPartialModels.BEARING_TOP_WOODEN : AllPartialModels.BEARING_TOP;
         SuperByteBuffer superBuffer = CachedBuffers.partial(top, be.getBlockState());
         float interpolatedAngle = be.getInterpolatedAngle(partialTicks - 1.0F);
         kineticRotationTransform(superBuffer, be, facing.getAxis(), (float)((double)(interpolatedAngle / 180.0F) * Math.PI), light);
         if (facing.getAxis().isHorizontal()) {
            superBuffer.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
         }

         superBuffer.rotateCentered(AngleHelper.rad((double)(-90.0F - AngleHelper.verticalAngle(facing))), Direction.EAST);
         superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }

   @Override
   protected SuperByteBuffer getRotatedModel(KineticBlockEntity be, BlockState state) {
      return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, ((Direction)state.getValue(BearingBlock.FACING)).getOpposite());
   }
}
