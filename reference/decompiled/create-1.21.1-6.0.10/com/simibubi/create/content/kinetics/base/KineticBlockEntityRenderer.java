package com.simibubi.create.content.kinetics.base;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferCache.Compartment;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.apache.commons.lang3.ArrayUtils;

public class KineticBlockEntityRenderer<T extends KineticBlockEntity> extends SafeBlockEntityRenderer<T> {
   public static final Compartment<BlockState> KINETIC_BLOCK = new Compartment();
   public static boolean rainbowMode = false;
   protected static final RenderType[] REVERSED_CHUNK_BUFFER_LAYERS = RenderType.chunkBufferLayers().toArray(RenderType[]::new);

   public KineticBlockEntityRenderer(Context context) {
   }

   protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         BlockState state = this.getRenderedBlockState(be);
         RenderType type = this.getRenderType(be, state);
         renderRotatingBuffer(be, this.getRotatedModel(be, state), ms, buffer.getBuffer(type), light);
      }
   }

   protected BlockState getRenderedBlockState(T be) {
      return be.getBlockState();
   }

   protected RenderType getRenderType(T be, BlockState state) {
      BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
      ChunkRenderTypeSet typeSet = model.getRenderTypes(state, RandomSource.create(42L), ModelData.EMPTY);

      for (RenderType type : REVERSED_CHUNK_BUFFER_LAYERS) {
         if (typeSet.contains(type)) {
            return type;
         }
      }

      return RenderType.cutoutMipped();
   }

   protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
      return CachedBuffers.block(KINETIC_BLOCK, state);
   }

   public static void renderRotatingKineticBlock(KineticBlockEntity be, BlockState renderedState, PoseStack ms, VertexConsumer buffer, int light) {
      SuperByteBuffer superByteBuffer = CachedBuffers.block(KINETIC_BLOCK, renderedState);
      renderRotatingBuffer(be, superByteBuffer, ms, buffer, light);
   }

   public static void renderRotatingBuffer(KineticBlockEntity be, SuperByteBuffer superBuffer, PoseStack ms, VertexConsumer buffer, int light) {
      standardKineticRotationTransform(superBuffer, be, light).renderInto(ms, buffer);
   }

   public static float getAngleForBe(KineticBlockEntity be, BlockPos pos, Axis axis) {
      float time = AnimationTickHolder.getRenderTime(be.getLevel());
      float offset = getRotationOffsetForPosition(be, pos, axis);
      return (time * be.getSpeed() * 3.0F / 10.0F + offset) % 360.0F / 180.0F * (float) Math.PI;
   }

   public static SuperByteBuffer standardKineticRotationTransform(SuperByteBuffer buffer, KineticBlockEntity be, int light) {
      BlockPos pos = be.getBlockPos();
      Axis axis = ((IRotate)be.getBlockState().getBlock()).getRotationAxis(be.getBlockState());
      return kineticRotationTransform(buffer, be, axis, getAngleForBe(be, pos, axis), light);
   }

   public static SuperByteBuffer kineticRotationTransform(SuperByteBuffer buffer, KineticBlockEntity be, Axis axis, float angle, int light) {
      buffer.light(light);
      buffer.rotateCentered(angle, Direction.get(AxisDirection.POSITIVE, axis));
      if (KineticDebugger.isActive()) {
         rainbowMode = true;
         buffer.color(be.hasNetwork() ? Color.generateFromLong(be.network) : Color.WHITE);
      } else {
         float overStressedEffect = be.effects.overStressedEffect;
         if (overStressedEffect != 0.0F) {
            boolean overstressed = overStressedEffect > 0.0F;
            Color color = overstressed ? Color.RED : Color.SPRING_GREEN;
            float weight = overstressed ? overStressedEffect : -overStressedEffect;
            buffer.color(Color.WHITE.mixWith(color, weight));
         } else {
            buffer.color(Color.WHITE);
         }
      }

      return buffer;
   }

   public static float getRotationOffsetForPosition(KineticBlockEntity be, BlockPos pos, Axis axis) {
      return KineticBlockEntityVisual.rotationOffset(be.getBlockState(), axis, pos) + (float)be.getRotationAngleOffset(axis);
   }

   public static BlockState shaft(Axis axis) {
      return (BlockState)AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
   }

   public static Axis getRotationAxisOf(KineticBlockEntity be) {
      return ((IRotate)be.getBlockState().getBlock()).getRotationAxis(be.getBlockState());
   }

   static {
      ArrayUtils.reverse(REVERSED_CHUNK_BUFFER_LAYERS);
   }
}
