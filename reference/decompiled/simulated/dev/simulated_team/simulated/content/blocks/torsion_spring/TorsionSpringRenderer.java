package dev.simulated_team.simulated.content.blocks.torsion_spring;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class TorsionSpringRenderer extends KineticBlockEntityRenderer<TorsionSpringBlockEntity> {
   public TorsionSpringRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(TorsionSpringBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         Direction facing = (Direction)be.getBlockState().getValue(TorsionSpringBlock.FACING);
         SuperByteBuffer spring = CachedBuffers.partial(SimPartialModels.TORSION_SPRING, be.getBlockState());
         float angle = be.interpolatedSpring(partialTicks);
         kineticRotationTransform(spring, be, facing.getAxis(), (float) (Math.PI / 180.0) * angle, light);
         if (facing.getAxis().isHorizontal()) {
            spring.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
         }

         spring.rotateCentered(AngleHelper.rad((double)(-90.0F - AngleHelper.verticalAngle(facing))), Direction.EAST);
         spring.renderInto(ms, buffer.getBuffer(RenderType.solid()));
         SuperByteBuffer shaftOut = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, be.getBlockState(), facing);
         kineticRotationTransform(shaftOut, be, facing.getAxis(), getAngleForBe(be.getExtraKinetics(), be.getBlockPos(), facing.getAxis()), light);
         shaftOut.renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }

   protected SuperByteBuffer getRotatedModel(TorsionSpringBlockEntity be, BlockState state) {
      return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, ((Direction)state.getValue(BearingBlock.FACING)).getOpposite());
   }
}
