package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.ryanhcode.sable.util.SableDistUtil;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.joml.Matrix4f;

public class AltitudeSensorRenderer extends SmartBlockEntityRenderer<AltitudeSensorBlockEntity> {
   public AltitudeSensorRenderer(Context context) {
      super(context);
   }

   public static float calculateLinearDial(float minHeight, float maxHeight, float height) {
      float fraction = (height - minHeight) / (maxHeight - minHeight);
      return Math.min(Math.max(fraction, 0.0F), 1.0F);
   }

   public static void render(
      BlockState blockState,
      int tickCount,
      float dialValue,
      float visualHeight,
      PoseStack poseStack,
      PoseStack contraptionPose,
      Matrix4f worldLight,
      MultiBufferSource bufferSource,
      int light
   ) {
      Level level = SableDistUtil.getClientLevel();
      VertexConsumer vb = bufferSource.getBuffer(RenderType.cutout());
      SuperByteBuffer indicator = CachedBuffers.partial(SimPartialModels.ALTITUDE_SENSOR_INDICATOR, blockState);
      PartialModel box = SimPartialModels.ALTITUDE_SENSOR_LINEAR_CASE;
      PartialModel dial = SimPartialModels.ALTITUDE_SENSOR_LINEAR_HAND;
      boolean isRadial = blockState.getValue(AltitudeSensorBlock.DIAL) == AltitudeSensorBlock.FaceType.RADIAL;
      if (isRadial) {
         box = SimPartialModels.ALTITUDE_SENSOR_RADIAL_CASE;
         dial = SimPartialModels.ALTITUDE_SENSOR_RADIAL_HAND;
      }

      SuperByteBuffer face = CachedBuffers.partial(box, blockState);
      SuperByteBuffer dialBuffer = CachedBuffers.partial(dial, blockState);
      Direction direction = (Direction)blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
      if (contraptionPose != null) {
         face.transform(contraptionPose);
         dialBuffer.transform(contraptionPose);
         indicator.transform(contraptionPose);
      }

      if (isRadial) {
         dialBuffer.rotateCentered(-((float)((double)visualHeight * Math.PI / 2.0)), direction);
      } else {
         dialBuffer.translate(0.0F, (dialValue * 8.0F - 4.0F) / 16.0F, 0.0F);
      }

      AttachFace attachFace = (AttachFace)blockState.getValue(AltitudeSensorBlock.FACE);
      float attachFaceAngle = attachFace == AttachFace.WALL ? 90.0F : (attachFace == AttachFace.CEILING ? 180.0F : 0.0F);
      float time = (float)tickCount + AnimationTickHolder.getPartialTicks();
      float wobbleAngle = (float)(-Math.sin((double)time * 0.8) * Math.exp((double)(-time) / 3.5)) * 0.7F;
      float yRot = !direction.getAxis().equals(Axis.Z)
         ? (float)Math.toRadians((double)((Direction)blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)).getOpposite().toYRot())
         : (float)Math.toRadians((double)((Direction)blockState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING)).toYRot());
      ((SuperByteBuffer)face.rotateCentered((float)((double)yRot + Math.PI), Direction.UP)).rotateCentered(wobbleAngle, Direction.WEST);
      ((SuperByteBuffer)dialBuffer.rotateCentered((float)((double)yRot + Math.PI), Direction.UP)).rotateCentered(wobbleAngle, Direction.WEST);
      ((SuperByteBuffer)indicator.rotateCentered((float)((double)yRot + Math.PI), Direction.UP))
         .rotateCentered((float)Math.toRadians((double)attachFaceAngle), Direction.WEST);
      if (worldLight != null) {
         face.useLevelLight(level, new Matrix4f(worldLight));
         dialBuffer.useLevelLight(level, new Matrix4f(worldLight));
         indicator.useLevelLight(level, new Matrix4f(worldLight));
      }

      face.light(light);
      dialBuffer.light(light);
      indicator.light(light);
      int color = SimColors.redstone(dialValue);
      indicator.color(color);
      face.renderInto(poseStack, vb);
      dialBuffer.renderInto(poseStack, vb);
      indicator.renderInto(poseStack, vb);
   }

   protected void renderSafe(AltitudeSensorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      render(be.getBlockState(), be.tickCount, be.getValue(), be.getVisualHeight(partialTicks), ms, null, null, buffer, light);
   }
}
