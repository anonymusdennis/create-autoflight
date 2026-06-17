package dev.simulated_team.simulated.content.blocks.throttle_lever;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.redstone.analogLever.AnalogLeverBlock;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.mixin.accessor.LevelRendererAccessor;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ThrottleLeverRenderer extends SafeBlockEntityRenderer<ThrottleLeverBlockEntity> {
   protected static final double ANGLE_LIMIT = 40.0;

   public ThrottleLeverRenderer(Context context) {
   }

   public static void transformHandleExternal(ThrottleLeverBlockEntity blockEntity, float partialTicks, PoseStack ms) {
      float state = blockEntity.clientAngle.getValue(partialTicks);
      AttachFace face = (AttachFace)blockEntity.getBlockState().getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
      float angle = (float)(((double)(state / 15.0F) * 80.0 - 40.0) / 180.0 * Math.PI);
      if (face == AttachFace.WALL) {
         angle = -angle;
      }

      PoseTransformStack stack = TransformStack.of(ms);
      transform(stack, blockEntity.getBlockState());
      ((PoseTransformStack)((PoseTransformStack)stack.translate(0.5, 0.1875, 0.5)).rotateX(angle)).translateBack(0.5, 0.1875, 0.5);
   }

   protected void renderSafe(ThrottleLeverBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
      BlockState leverState = be.getBlockState();
      float state = be.clientAngle.getValue(partialTicks);
      AttachFace face = (AttachFace)be.getBlockState().getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
      float angle = (float)(((double)(state / 15.0F) * 80.0 - 40.0) / 180.0 * Math.PI);
      if (face == AttachFace.WALL) {
         angle = -angle;
      }

      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         VertexConsumer vb = bufferSource.getBuffer(RenderType.cutoutMipped());
         SuperByteBuffer handle = CachedBuffers.partial(SimPartialModels.THROTTLE_LEVER_HANDLE, leverState);
         SuperByteBuffer button = CachedBuffers.partial(SimPartialModels.THROTTLE_LEVER_BUTTON, leverState);
         float signalStrength = Math.max(0.0F, (float)be.state / 15.0F);
         SuperByteBuffer diode = CachedBuffers.partial(SimPartialModels.THROTTLE_LEVER_DIODE, leverState);
         int color = SimColors.redstone(signalStrength);
         double buttonAngle = (double)(be.clientPressedLerp.getValue(partialTicks) * -7.0F);
         transform(handle, leverState);
         transform(button, leverState);
         transform(diode, leverState);
         this.transformHandleExternal(handle, angle, face);
         handle.light(light).renderInto(ms, vb);
         ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)this.transformHandleExternal(button, angle, face).translate(0.0F, 0.875F, 0.5F))
                  .rotateXDegrees((float)buttonAngle))
               .translateBack(0.0F, 0.875F, 0.5F))
            .light(light)
            .renderInto(ms, vb);
         diode.light(light).color(color).renderInto(ms, vb);
      }

      Minecraft minecraft = Minecraft.getInstance();
      if (!be.isVirtual() && minecraft.hitResult instanceof BlockHitResult hitResult && hitResult.getBlockPos().equals(be.getBlockPos())) {
         renderOutline(be, ms, bufferSource, angle);
      }
   }

   private static void renderOutline(ThrottleLeverBlockEntity be, PoseStack ms, MultiBufferSource bufferSource, float angle) {
      VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
      VoxelShape leverShape = ((ThrottleLeverBlock)SimBlocks.THROTTLE_LEVER.get()).getHandleShape(SimBlocks.THROTTLE_LEVER.getDefaultState());
      ms.pushPose();
      PoseTransformStack stack = TransformStack.of(ms);
      transform(stack, be.getBlockState());
      ((PoseTransformStack)((PoseTransformStack)stack.translate(0.5, 0.1875, 0.5)).rotateX(angle)).translateBack(0.5, 0.1875, 0.5);
      LevelRendererAccessor.invokeRenderShape(ms, consumer, leverShape, 0.0, 0.0, 0.0, 0.0F, 0.0F, 0.0F, 0.4F);
      ms.popPose();
   }

   private <T extends TransformStack<T>> TransformStack<T> transformHandleExternal(TransformStack<T> buffer, float angle, AttachFace face) {
      return (TransformStack<T>)((TransformStack)((TransformStack)((TransformStack)buffer.translate(0.5F, 0.1875F, 0.5F)).rotateX(angle))
            .translateBack(0.5F, 0.1875F, 0.5F))
         .rotateCentered(face == AttachFace.WALL ? (float) Math.PI : 0.0F, Direction.UP);
   }

   private static <T extends TransformStack<T>> TransformStack<T> transform(TransformStack<T> buffer, BlockState leverState) {
      AttachFace attached = (AttachFace)leverState.getValue(AnalogLeverBlock.FACE);
      Direction facing = (Direction)leverState.getValue(AnalogLeverBlock.FACING);

      float rX = switch (attached) {
         case FLOOR -> 0.0F;
         case WALL -> 90.0F;
         default -> 180.0F;
      };
      float rY = AngleHelper.horizontalAngle(facing);
      buffer.rotateCentered((float)((double)(rY / 180.0F) * Math.PI), Direction.UP);
      buffer.rotateCentered((float)((double)(rX / 180.0F) * Math.PI), Direction.EAST);
      buffer.rotateCentered(attached == AttachFace.CEILING ? (float) Math.PI : 0.0F, Direction.UP);
      return buffer;
   }
}
