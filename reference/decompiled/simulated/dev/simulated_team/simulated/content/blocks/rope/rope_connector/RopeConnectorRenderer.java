package dev.simulated_team.simulated.content.blocks.rope.rope_connector;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class RopeConnectorRenderer extends SafeBlockEntityRenderer<RopeConnectorBlockEntity> {
   public RopeConnectorRenderer(Context context) {
   }

   public boolean shouldRenderOffScreen(RopeConnectorBlockEntity blockEntity) {
      return true;
   }

   public boolean shouldRender(RopeConnectorBlockEntity blockEntity, Vec3 cameraPos) {
      return true;
   }

   protected void renderSafe(RopeConnectorBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      RopeStrandRenderer.render(be, be.getRopeHolder(), partialTicks, ms, buffer);
      RopeStrandHolderBehavior holder = be.getRopeHolder();
      if (holder.isAttached() || be.isVirtual() && be.getRopeHolder().renderAttached) {
         SuperByteBuffer knot = CachedBuffers.partialFacing(SimPartialModels.ROPE_CONNECTOR_KNOT, AllBlocks.ROPE.getDefaultState(), Direction.NORTH);
         BlockPos blockPos = be.getBlockPos();
         BlockState state = be.getBlockState();
         Vec3 attachmentPoint = be.getVisualAttachmentPoint(blockPos, state);
         Direction facing = (Direction)state.getValue(RopeConnectorBlock.FACING);
         SuperByteBuffer knotBuffer = knot.light(light);
         boolean axisAlongFirstCoordinate = (Boolean)state.getValue(RopeConnectorBlock.AXIS_ALONG_FIRST_COORDINATE);
         float zRotLast = axisAlongFirstCoordinate ^ facing.getAxis() == Axis.Z ? 90.0F : 0.0F;
         float yRot = AngleHelper.horizontalAngle(facing) + (!axisAlongFirstCoordinate && facing.getAxis() == Axis.Y ? 90.0F : 0.0F);
         float zRot = facing == Direction.UP ? 270.0F : (facing == Direction.DOWN ? 90.0F : 0.0F);
         knotBuffer.translate(attachmentPoint.subtract(blockPos.getCenter()));
         knotBuffer.rotateCentered((float)((double)(zRot / 180.0F) * Math.PI), Direction.SOUTH);
         knotBuffer.rotateCentered((float)((double)(yRot / 180.0F) * Math.PI), Direction.UP);
         knotBuffer.rotateCentered((float)((double)(zRotLast / 180.0F) * Math.PI), Direction.SOUTH);
         knotBuffer.rotateCentered((float) (Math.PI / 2), Direction.UP);
         knotBuffer.renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }
}
