package dev.ryanhcode.sable.neoforge.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.neoforge.compatibility.flywheel.FlywheelCompatNeoForge;
import dev.ryanhcode.sable.platform.SableSubLevelRenderPlatform;
import dev.ryanhcode.sable.sublevel.render.vanilla.SingleBlockSubLevelWrapper;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SableSubLevelRenderPlatformImpl implements SableSubLevelRenderPlatform {
   @Override
   public void tesselateBlock(
      SingleBlockSubLevelWrapper blockAndTintGetter,
      BakedModel bakedModel,
      BlockState blockState,
      BlockPos pos,
      PoseStack poseStack,
      VertexConsumer vertexConsumer,
      RandomSource randomSource,
      long seed,
      int packedOverlay,
      @Nullable RenderType renderType
   ) {
      Minecraft.getInstance()
         .getBlockRenderer()
         .modelRenderer
         .tesselateWithoutAO(
            blockAndTintGetter,
            bakedModel,
            blockState,
            pos,
            poseStack,
            vertexConsumer,
            true,
            randomSource,
            seed,
            packedOverlay,
            blockAndTintGetter.getLevel().getModelData(pos),
            renderType
         );
   }

   @Override
   public List<RenderType> getRenderLayers(
      SingleBlockSubLevelWrapper blockAndTintGetter, BakedModel bakedModel, BlockState blockState, BlockPos pos, RandomSource randomSource
   ) {
      return bakedModel.getRenderTypes(blockState, randomSource, blockAndTintGetter.getModelData(pos)).asList();
   }

   @Override
   public void tryAddFlywheelVisual(BlockEntity blockEntity) {
      if (FlywheelCompatNeoForge.FLYWHEEL_LOADED) {
         FlywheelCompatNeoForge.tryAddVisual(blockEntity);
      }
   }
}
