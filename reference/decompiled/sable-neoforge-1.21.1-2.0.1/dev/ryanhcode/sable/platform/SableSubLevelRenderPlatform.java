package dev.ryanhcode.sable.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.sublevel.render.vanilla.SingleBlockSubLevelWrapper;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface SableSubLevelRenderPlatform {
   SableSubLevelRenderPlatform INSTANCE = SablePlatformUtil.load(SableSubLevelRenderPlatform.class);

   void tesselateBlock(
      SingleBlockSubLevelWrapper var1,
      BakedModel var2,
      BlockState var3,
      BlockPos var4,
      PoseStack var5,
      VertexConsumer var6,
      RandomSource var7,
      long var8,
      int var10,
      @Nullable RenderType var11
   );

   List<RenderType> getRenderLayers(SingleBlockSubLevelWrapper var1, BakedModel var2, BlockState var3, BlockPos var4, RandomSource var5);

   void tryAddFlywheelVisual(BlockEntity var1);
}
