package dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class MountedPotatoCannonRenderer extends SafeBlockEntityRenderer<MountedPotatoCannonBlockEntity> {
   public MountedPotatoCannonRenderer(Context context) {
   }

   protected void renderSafe(MountedPotatoCannonBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
      this.renderComponents(be, partialTicks, ms, buffer, light, overlay);
      this.renderItem(be, partialTicks, ms, buffer, light, overlay);
   }

   private void renderComponents(MountedPotatoCannonBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
      boolean drawParts = !VisualizationManager.supportsVisualization(be.getLevel());
      if (drawParts) {
         KineticBlockEntityRenderer.renderRotatingKineticBlock(be, this.getRenderedBlockState(be), ms, vb, light);
      }

      BlockState blockState = be.getBlockState();
      float barrelOffset = !be.isBlocked() ? be.getBarrelDistance(partialTicks) : (float)(-(be.getBlockedLength() / 2.0));
      float bellowOffset = -be.getBellowDistance(partialTicks);
      SuperByteBuffer barrel = CachedBuffers.partial(AeroPartialModels.CANNON_BARREL, blockState);
      ((SuperByteBuffer)transform(barrel, blockState, true).translate(0.0F, 0.0F, barrelOffset)).light(light).renderInto(ms, vb);
      SuperByteBuffer bellow = CachedBuffers.partial(AeroPartialModels.CANNON_BELLOW, blockState);
      ((SuperByteBuffer)transform(bellow, blockState, true).translate(0.0F, bellowOffset, 0.0F)).light(light).renderInto(ms, vb);
      ((SuperByteBuffer)((SuperByteBuffer)transform(bellow, blockState, true).rotateCentered((float) Math.PI, Direction.SOUTH))
            .light(light)
            .translate(0.0F, bellowOffset, 0.0F))
         .renderInto(ms, vb);
      if (drawParts) {
         SuperByteBuffer cogwheel = CachedBuffers.partial(AeroPartialModels.CANNON_COG, blockState);
         float angle = be.getCogwheelAngle(partialTicks);
         ((SuperByteBuffer)transform(cogwheel, blockState, true).rotateCentered((float) (Math.PI / 180.0) * (angle % 360.0F), Direction.SOUTH))
            .light(light)
            .renderInto(ms, vb);
      }
   }

   private static SuperByteBuffer transform(SuperByteBuffer buffer, BlockState state, boolean axisDirectionMatters) {
      Direction facing = (Direction)state.getValue(BlockStateProperties.FACING);
      float zRotLast = axisDirectionMatters && state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) ^ facing.getAxis() == Axis.Z
         ? 90.0F
         : 0.0F;
      float yRot = AngleHelper.horizontalAngle(facing);
      float zRot = facing == Direction.UP ? 90.0F : (facing == Direction.DOWN ? 90.0F : 0.0F);
      float zRotSecondLast = facing == Direction.UP ? 180.0F : 0.0F;
      buffer.rotateCentered((float)((double)(zRot / 180.0F) * Math.PI), Direction.SOUTH);
      buffer.rotateCentered((float)((double)(zRot / 180.0F) * Math.PI), Direction.DOWN);
      buffer.rotateCentered((float)((double)(yRot / 180.0F) * Math.PI), Direction.UP);
      buffer.rotateCentered((float)((double)(zRotLast / 180.0F) * Math.PI), Direction.SOUTH);
      buffer.rotateCentered((float)((double)(zRotSecondLast / 180.0F) * Math.PI), Direction.UP);
      return buffer;
   }

   private BlockState getRenderedBlockState(MountedPotatoCannonBlockEntity te) {
      return KineticBlockEntityRenderer.shaft(KineticBlockEntityRenderer.getRotationAxisOf(te));
   }

   public void renderItem(MountedPotatoCannonBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!be.getInventory().isEmpty()) {
         ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
         TransformStack<PoseTransformStack> msr = TransformStack.of(ms);
         ms.pushPose();
         msr.center();
         Direction facing = (Direction)be.getBlockState().getValue(BlockStateProperties.FACING);
         Vec3i facingVec = facing.getNormal();
         float itemScale = 0.35F;
         float normalizedTimer = be.getItemTime(partialTicks);
         float itemPosition = !be.isBlocked() ? 1.0F - (float)Math.exp((double)(-0.25F * normalizedTimer)) : 0.0F;
         itemPosition *= 0.8F;
         ms.translate((float)facingVec.getX() * itemPosition, (float)facing.getStepY() * itemPosition, (float)facingVec.getZ() * itemPosition);
         ms.scale(0.35F, 0.35F, 0.35F);
         int itemRotationId = be.getItemRotationId();
         Quaternionf Q = new Quaternionf(
            (float)Math.sin((double)((float)itemRotationId * 0.4F)),
            (float)Math.cos((double)((float)itemRotationId * 1.4F)),
            (float)Math.sin((double)((float)itemRotationId * 3.0F)),
            (float)Math.cos((double)((float)itemRotationId * 5.0F))
         );
         Q.normalize();
         msr.rotate(Q);
         itemRenderer.renderStatic(be.getInventory().slot.getStack(), ItemDisplayContext.FIXED, light, overlay, ms, buffer, be.getLevel(), 0);
         ms.popPose();
      }
   }
}
