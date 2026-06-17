package com.simibubi.create.content.contraptions.actors.psi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.function.Consumer;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class PortableStorageInterfaceRenderer extends SafeBlockEntityRenderer<PortableStorageInterfaceBlockEntity> {
   public PortableStorageInterfaceRenderer(Context context) {
   }

   protected void renderSafe(PortableStorageInterfaceBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState blockState = be.getBlockState();
         float progress = be.getExtensionDistance(partialTicks);
         VertexConsumer vb = buffer.getBuffer(RenderType.solid());
         render(blockState, be.isConnected(), progress, null, sbb -> sbb.light(light).renderInto(ms, vb));
      }
   }

   public static void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
      BlockState blockState = context.state;
      VertexConsumer vb = buffer.getBuffer(RenderType.solid());
      float renderPartialTicks = AnimationTickHolder.getPartialTicks();
      LerpedFloat animation = PortableStorageInterfaceMovement.getAnimation(context);
      float progress = animation.getValue(renderPartialTicks);
      boolean lit = animation.settled();
      render(
         blockState,
         lit,
         progress,
         matrices.getModel(),
         sbb -> sbb.light(LevelRenderer.getLightColor(renderWorld, context.localPos))
               .useLevelLight(context.world, matrices.getWorld())
               .renderInto(matrices.getViewProjection(), vb)
      );
   }

   private static void render(BlockState blockState, boolean lit, float progress, PoseStack local, Consumer<SuperByteBuffer> drawCallback) {
      SuperByteBuffer middle = CachedBuffers.partial(getMiddleForState(blockState, lit), blockState);
      SuperByteBuffer top = CachedBuffers.partial(getTopForState(blockState), blockState);
      if (local != null) {
         middle.transform(local);
         top.transform(local);
      }

      Direction facing = (Direction)blockState.getValue(PortableStorageInterfaceBlock.FACING);
      rotateToFacing(middle, facing);
      rotateToFacing(top, facing);
      middle.translate(0.0F, progress * 0.5F + 0.375F, 0.0F);
      top.translate(0.0F, progress, 0.0F);
      drawCallback.accept(middle);
      drawCallback.accept(top);
   }

   private static void rotateToFacing(SuperByteBuffer buffer, Direction facing) {
      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)buffer.center()).rotateYDegrees(AngleHelper.horizontalAngle(facing)))
            .rotateXDegrees(facing == Direction.UP ? 0.0F : (facing == Direction.DOWN ? 180.0F : 90.0F)))
         .uncenter();
   }

   static PortableStorageInterfaceBlockEntity getTargetPSI(MovementContext context) {
      String _workingPos_ = "WorkingPos";
      if (!context.data.contains(_workingPos_)) {
         return null;
      } else {
         BlockPos pos = NBTHelper.readBlockPos(context.data, _workingPos_);
         if (context.world.getBlockEntity(pos) instanceof PortableStorageInterfaceBlockEntity psi) {
            return !psi.isTransferring() ? null : psi;
         } else {
            return null;
         }
      }
   }

   static PartialModel getMiddleForState(BlockState state, boolean lit) {
      if (AllBlocks.PORTABLE_FLUID_INTERFACE.has(state)) {
         return lit ? AllPartialModels.PORTABLE_FLUID_INTERFACE_MIDDLE_POWERED : AllPartialModels.PORTABLE_FLUID_INTERFACE_MIDDLE;
      } else {
         return lit ? AllPartialModels.PORTABLE_STORAGE_INTERFACE_MIDDLE_POWERED : AllPartialModels.PORTABLE_STORAGE_INTERFACE_MIDDLE;
      }
   }

   static PartialModel getTopForState(BlockState state) {
      return AllBlocks.PORTABLE_FLUID_INTERFACE.has(state) ? AllPartialModels.PORTABLE_FLUID_INTERFACE_TOP : AllPartialModels.PORTABLE_STORAGE_INTERFACE_TOP;
   }
}
