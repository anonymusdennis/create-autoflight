package dev.engine_room.flywheel.lib.model.baked;

import dev.engine_room.flywheel.lib.model.SimpleModel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class ModelBuilderImpl {
   private ModelBuilderImpl() {
   }

   public static SimpleModel buildBakedModelBuilder(BakedModelBuilder builder) {
      BlockState blockState = builder.level.getBlockState(builder.pos);
      return BakedModelBufferer.bufferModel(builder.bakedModel, builder.pos, builder.level, blockState, builder.poseStack, builder.materialFunc);
   }

   public static SimpleModel buildBlockModelBuilder(BlockModelBuilder builder) {
      return BakedModelBufferer.bufferBlocks(builder.positions.iterator(), builder.level, builder.poseStack, builder.renderFluids, builder.materialFunc);
   }
}
