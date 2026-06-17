package dev.engine_room.flywheel.lib.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.BlockModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.model.baked.SinglePosVirtualBlockGetter;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class Models {
   private static final RendererReloadCache<BlockState, Model> BLOCK_STATE = new RendererReloadCache<>(
      it -> new BlockModelBuilder(SinglePosVirtualBlockGetter.createFullDark().blockState(it), List.of(BlockPos.ZERO)).build()
   );
   private static final RendererReloadCache<PartialModel, Model> PARTIAL = new RendererReloadCache<>(it -> new BakedModelBuilder(it.get()).build());
   private static final RendererReloadCache<Models.TransformedPartial<?>, Model> TRANSFORMED_PARTIAL = new RendererReloadCache<>(
      Models.TransformedPartial::create
   );

   private Models() {
   }

   public static Model block(BlockState state) {
      return BLOCK_STATE.get(state);
   }

   public static Model partial(PartialModel partial) {
      return PARTIAL.get(partial);
   }

   public static <T> Model partial(PartialModel partial, T key, BiConsumer<T, PoseStack> transformer) {
      return TRANSFORMED_PARTIAL.get(new Models.TransformedPartial<>(partial, key, transformer));
   }

   public static Model partial(PartialModel partial, Direction dir) {
      return partial(partial, dir, Models::rotateAboutCenterToFace);
   }

   private static void rotateAboutCenterToFace(Direction facing, PoseStack stack) {
      TransformStack.of(stack).center().rotateToFace(facing.getOpposite()).uncenter();
   }

   private static record TransformedPartial<T>(PartialModel partial, T key, BiConsumer<T, PoseStack> transformer) {
      private Model create() {
         PoseStack stack = new PoseStack();
         this.transformer.accept(this.key, stack);
         return new BakedModelBuilder(this.partial.get()).poseStack(stack).build();
      }
   }
}
