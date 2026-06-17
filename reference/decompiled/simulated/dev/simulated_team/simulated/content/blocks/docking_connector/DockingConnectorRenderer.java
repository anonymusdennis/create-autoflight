package dev.simulated_team.simulated.content.blocks.docking_connector;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public class DockingConnectorRenderer extends SafeBlockEntityRenderer<DockingConnectorBlockEntity> {
   public DockingConnectorRenderer(Context context) {
   }

   protected void renderSafe(DockingConnectorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
      VertexConsumer vb = bufferSource.getBuffer(RenderType.cutout());
      Direction direction = (Direction)be.getBlockState().getValue(BlockStateProperties.FACING);
      BlockState blockState = be.getBlockState();
      float extension = be.getExtensionDistance(partialTicks);
      float rotation = be.getFeetRotation(partialTicks) * 90.0F;
      SuperByteBuffer piston1 = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_MAIN_PISTON_BOTTOM, blockState);
      SuperByteBuffer piston2 = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_MAIN_PISTON_TOP, blockState);
      SuperByteBuffer sidePiston1 = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_SIDE_PISTON_BOTTOM, blockState);
      SuperByteBuffer sidePiston2 = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_SIDE_PISTON_TOP, blockState);
      SuperByteBuffer foot = CachedBuffers.partial(SimPartialModels.DOCKING_CONNECTOR_FOOT, blockState);
      ms.pushPose();
      rotateToFaceCentered(ms, direction);
      piston1.translate(0.0, (double)extension * 0.5, 0.0);
      piston2.translate(0.0F, extension, 0.0F);
      piston1.light(light).renderInto(ms, vb);
      piston2.light(light).renderInto(ms, vb);
      Vector2f footAnchor = new Vector2f();
      Vector2f sidePistonTopAnchor = new Vector2f();
      Vector2f sidePistonBottomAnchor = new Vector2f();
      Vector2f relativeAnchor = new Vector2f();
      footAnchor.set(-7.5F, 15.5F).div(16.0F).add(0.0F, extension);
      this.rotateVector2f(sidePistonTopAnchor.set(1.5F, -2.5F).div(16.0F), rotation).add(footAnchor);
      sidePistonBottomAnchor.set(-6.0F, 2.0F).div(16.0F).add(0.0F, extension / 2.0F);
      relativeAnchor.set(sidePistonTopAnchor).sub(sidePistonBottomAnchor);
      relativeAnchor.normalize();
      Matrix4f rotationMatrix = new Matrix4f(
         1.0F, 0.0F, 0.0F, 0.0F, 0.0F, relativeAnchor.y, relativeAnchor.x, 0.0F, 0.0F, -relativeAnchor.x, relativeAnchor.y, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F
      );

      for (int i = 0; i < 4; i++) {
         ms.pushPose();
         ms.translate(0.5, 0.0, 0.5);
         TransformStack.of(ms).rotateYDegrees((float)(i * 90));
         sidePiston1.translate(0.0F, sidePistonBottomAnchor.y, sidePistonBottomAnchor.x);
         sidePiston2.translate(0.0F, sidePistonTopAnchor.y, sidePistonTopAnchor.x);
         foot.translate(0.0F, footAnchor.y, footAnchor.x);
         foot.rotateXDegrees(rotation);
         sidePiston1.mulPose(rotationMatrix);
         sidePiston2.mulPose(rotationMatrix);
         sidePiston1.light(light).renderInto(ms, vb);
         sidePiston2.light(light).renderInto(ms, vb);
         foot.light(light).renderInto(ms, vb);
         ms.popPose();
      }

      ms.popPose();
   }

   public int getViewDistance() {
      return 256;
   }

   public static void rotateToFaceCentered(PoseStack ms, Direction facing) {
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(ms).center()).rotateYDegrees(AngleHelper.horizontalAngle(facing)))
            .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90.0F))
         .uncenter();
   }

   private Vector2f rotateVector2f(Vector2f v, float angle) {
      angle = (float)Math.toRadians((double)angle);
      float s = Mth.sin(angle);
      float c = Mth.cos(angle);
      v.set(v.x * c + v.y * s, v.y * c - v.x * s);
      return v;
   }
}
