package net.createmod.catnip.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.function.Supplier;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.apache.commons.lang3.tuple.Pair;

public class CachedBuffers {
   public static final SuperByteBufferCache.Compartment<BlockState> GENERIC_BLOCK = new SuperByteBufferCache.Compartment<>();
   public static final SuperByteBufferCache.Compartment<PartialModel> PARTIAL = new SuperByteBufferCache.Compartment<>();
   public static final SuperByteBufferCache.Compartment<Pair<Direction, PartialModel>> DIRECTIONAL_PARTIAL = new SuperByteBufferCache.Compartment<>();

   public static SuperByteBuffer block(BlockState toRender) {
      return block(GENERIC_BLOCK, toRender);
   }

   public static SuperByteBuffer block(SuperByteBufferCache.Compartment<BlockState> compartment, BlockState toRender) {
      return SuperByteBufferCache.getInstance().get(compartment, toRender, () -> SuperBufferFactory.getInstance().createForBlock(toRender));
   }

   public static SuperByteBuffer partial(PartialModel partial, BlockState referenceState) {
      return SuperByteBufferCache.getInstance().get(PARTIAL, partial, () -> SuperBufferFactory.getInstance().createForBlock(partial.get(), referenceState));
   }

   public static SuperByteBuffer partial(PartialModel partial, BlockState referenceState, Supplier<PoseStack> modelTransform) {
      return SuperByteBufferCache.getInstance()
         .get(PARTIAL, partial, () -> SuperBufferFactory.getInstance().createForBlock(partial.get(), referenceState, modelTransform.get()));
   }

   public static SuperByteBuffer partialFacing(PartialModel partial, BlockState referenceState) {
      Direction facing = (Direction)referenceState.getValue(BlockStateProperties.FACING);
      return partialFacing(partial, referenceState, facing);
   }

   public static SuperByteBuffer partialFacing(PartialModel partial, BlockState referenceState, Direction facing) {
      return partialDirectional(partial, referenceState, facing, rotateToFace(facing));
   }

   public static SuperByteBuffer partialFacingVertical(PartialModel partial, BlockState referenceState, Direction facing) {
      return partialDirectional(partial, referenceState, facing, rotateToFaceVertical(facing));
   }

   public static SuperByteBuffer partialDirectional(PartialModel partial, BlockState referenceState, Direction dir, Supplier<PoseStack> modelTransform) {
      return SuperByteBufferCache.getInstance()
         .get(
            DIRECTIONAL_PARTIAL,
            Pair.of(dir, partial),
            () -> SuperBufferFactory.getInstance().createForBlock(partial.get(), referenceState, modelTransform.get())
         );
   }

   public static Supplier<PoseStack> rotateToFace(Direction facing) {
      return () -> {
         PoseStack stack = new PoseStack();
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(stack).center()).rotateYDegrees(AngleHelper.horizontalAngle(facing)))
               .rotateXDegrees(AngleHelper.verticalAngle(facing)))
            .uncenter();
         return stack;
      };
   }

   public static Supplier<PoseStack> rotateToFaceVertical(Direction facing) {
      return () -> {
         PoseStack stack = new PoseStack();
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(stack).center()).rotateYDegrees(AngleHelper.horizontalAngle(facing)))
               .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90.0F))
            .uncenter();
         return stack;
      };
   }
}
