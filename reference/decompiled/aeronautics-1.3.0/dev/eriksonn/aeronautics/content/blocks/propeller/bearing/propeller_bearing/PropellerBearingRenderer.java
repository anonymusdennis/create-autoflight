package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class PropellerBearingRenderer extends KineticBlockEntityRenderer<PropellerBearingBlockEntity> {
   public PropellerBearingRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(PropellerBearingBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         Direction facing = (Direction)be.getBlockState().getValue(BlockStateProperties.FACING);
         PartialModel top = AeroPartialModels.BEARING_PLATE;
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

   protected SuperByteBuffer getRotatedModel(PropellerBearingBlockEntity te, BlockState state) {
      return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, ((Direction)state.getValue(BearingBlock.FACING)).getOpposite());
   }
}
