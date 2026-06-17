package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class GyroscopicPropellerBearingRenderer extends KineticBlockEntityRenderer<GyroscopicPropellerBearingBlockEntity> {
   public GyroscopicPropellerBearingRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(GyroscopicPropellerBearingBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         Direction facing = (Direction)be.getBlockState().getValue(BlockStateProperties.FACING);
         Vec3 normal = new Vec3((double)facing.getStepX(), (double)facing.getStepY(), (double)facing.getStepZ());
         Quaternionf tiltQuat = new Quaternionf(be.previousTiltQuat).slerp(be.tiltQuat, partialTicks);
         Quaternionf Q = new Quaternionf(tiltQuat);
         Q.conjugate();
         Q.mul(new Quaternionf((float)normal.x, (float)normal.y, (float)normal.z, 0.0F));
         Q.mul(tiltQuat);
         Vec3 contraptionNormal = new Vec3((double)Q.x(), (double)Q.y(), (double)Q.z());
         PartialModel top = AeroPartialModels.BEARING_PLATE_METAL;
         SuperByteBuffer superBuffer = CachedBuffers.partial(top, be.getBlockState());
         superBuffer.translate(normal.scale(0.25));
         superBuffer.rotateCentered(tiltQuat);
         superBuffer.translate(normal.scale(-0.25));
         float interpolatedAngle = be.getInterpolatedAngle(partialTicks - 1.0F);
         kineticRotationTransform(superBuffer, be, facing.getAxis(), (float)((double)(interpolatedAngle / 180.0F) * Math.PI), light);
         if (facing.getAxis().isHorizontal()) {
            superBuffer.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
         }

         superBuffer.rotateCentered(AngleHelper.rad((double)(-90.0F - AngleHelper.verticalAngle(facing))), Direction.EAST);
         superBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));

         for (int i = 0; i < 4; i++) {
            SuperByteBuffer headBuffer = CachedBuffers.partial(AeroPartialModels.GYRO_BEARING_PISTON_HEAD, be.getBlockState());
            SuperByteBuffer poleBuffer = CachedBuffers.partial(AeroPartialModels.GYRO_BEARING_PISTON_POLE, be.getBlockState());
            Vec3 originalPos = VecHelper.rotate(new Vec3(0.36875, 0.0, 0.0), (double)(-90 * i), Axis.Y);
            Vec3 translatedPos = originalPos;
            if (facing.getAxis().isHorizontal()) {
               translatedPos = VecHelper.rotate(originalPos, (double)AngleHelper.horizontalAngle(facing), Axis.Z);
               translatedPos = VecHelper.rotate(translatedPos, (double)(-90.0F + AngleHelper.verticalAngle(facing)), Axis.X);
            }

            double translateDistance = translatedPos.dot(contraptionNormal) / normal.dot(contraptionNormal);
            translatedPos = translatedPos.add(normal.scale(translateDistance + 0.1875));
            headBuffer.translate(translatedPos);
            headBuffer.translate(0.5F, 0.5F, 0.5F);
            poleBuffer.translate(translatedPos);
            poleBuffer.translate(0.5F, 0.5F, 0.5F);
            headBuffer.rotate(tiltQuat);
            int j = i;
            if (facing == Direction.DOWN) {
               if (i % 2 == 0) {
                  headBuffer.rotate(AngleHelper.rad(180.0), Direction.EAST);
                  poleBuffer.rotate(AngleHelper.rad(180.0), Direction.EAST);
               } else {
                  headBuffer.rotate(AngleHelper.rad(180.0), Direction.SOUTH);
                  poleBuffer.rotate(AngleHelper.rad(180.0), Direction.SOUTH);
               }
            }

            if (facing.getAxis().isHorizontal()) {
               headBuffer.rotate(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
               poleBuffer.rotate(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
               headBuffer.rotate(AngleHelper.rad((double)(-90.0F + AngleHelper.verticalAngle(facing))), Direction.EAST);
               poleBuffer.rotate(AngleHelper.rad((double)(-90.0F + AngleHelper.verticalAngle(facing))), Direction.EAST);
               j = 2 - i;
            }

            poleBuffer.translate(0.0, 0.03125, 0.0);
            headBuffer.rotate(AngleHelper.rad((double)(-90 * j)), Direction.UP);
            poleBuffer.rotate(AngleHelper.rad((double)(-90 * j)), Direction.UP);
            headBuffer.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
            poleBuffer.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
         }
      }
   }

   protected SuperByteBuffer getRotatedModel(GyroscopicPropellerBearingBlockEntity be, BlockState state) {
      return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, ((Direction)state.getValue(BearingBlock.FACING)).getOpposite());
   }
}
