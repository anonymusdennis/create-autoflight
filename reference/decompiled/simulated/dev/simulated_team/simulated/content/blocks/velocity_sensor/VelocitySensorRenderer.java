package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class VelocitySensorRenderer extends SafeBlockEntityRenderer<VelocitySensorBlockEntity> {
   public VelocitySensorRenderer(Context context) {
   }

   protected void renderSafe(VelocitySensorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
      BlockState state = be.getBlockState();
      SuperByteBuffer diode = CachedBuffers.partial(SimPartialModels.VELOCITY_SENSOR_DIODE, state);
      SuperByteBuffer fan = CachedBuffers.partial(SimPartialModels.VELOCITY_SENSOR_FAN, state);
      boolean front = (Integer)state.getValue(VelocitySensorBlock.POWERED) == 1;
      boolean axis = (Boolean)state.getValue(VelocitySensorBlock.AXIS_ALONG_FIRST_COORDINATE);

      front = switch ((Direction)state.getValue(VelocitySensorBlock.FACING)) {
         case NORTH, EAST -> !front;
         case SOUTH, DOWN -> axis == front;
         case WEST -> axis != front;
         case UP -> front;
         default -> throw new MatchException(null, null);
      };
      float signalStrength = (float)be.getRedstoneStrength() / 15.0F;
      int color = SimColors.redstone(signalStrength);
      this.transform(diode, state);
      diode.light(light).color(front ? color : SimColors.REDSTONE_OFF).renderInto(ms, vb);
      this.transform(diode, state);
      diode.rotateCenteredDegrees(180.0F, Axis.Y);
      diode.light(light).color(front ? SimColors.REDSTONE_OFF : color).renderInto(ms, vb);
      this.transform((SuperByteBuffer)fan.rotateCentered(be.getFanAngle(partialTicks), AbstractDirectionalAxisBlock.getDirectionOfAxis(state)), state);
      fan.light(light).renderInto(ms, vb);
   }

   private void transform(SuperByteBuffer diode, BlockState state) {
      Direction dir = (Direction)state.getValue(VelocitySensorBlock.FACING);
      boolean axis = (Boolean)state.getValue(VelocitySensorBlock.AXIS_ALONG_FIRST_COORDINATE);
      if (axis == (dir.getStepX() == 0)) {
         diode.rotateCenteredDegrees(90.0F, (Direction)state.getValue(VelocitySensorBlock.FACING));
      }

      diode.rotateCentered(dir.getRotation());
   }
}
