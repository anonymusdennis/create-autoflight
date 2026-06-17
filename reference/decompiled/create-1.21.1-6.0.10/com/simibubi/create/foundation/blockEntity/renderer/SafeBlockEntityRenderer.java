package com.simibubi.create.foundation.blockEntity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import com.simibubi.create.foundation.mixin.accessor.LevelRendererAccessor;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public abstract class SafeBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
   public final void render(T be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
      if (!this.isInvalid(be)) {
         this.renderSafe(be, partialTicks, ms, bufferSource, light, overlay);
      }
   }

   protected abstract void renderSafe(T var1, float var2, PoseStack var3, MultiBufferSource var4, int var5, int var6);

   public boolean isInvalid(T be) {
      return !be.hasLevel() || be.getBlockState().getBlock() == Blocks.AIR;
   }

   public boolean shouldCullItem(Vec3 itemPos, Level level) {
      if (level instanceof PonderLevel) {
         return false;
      } else {
         LevelRendererAccessor accessor = (LevelRendererAccessor)Minecraft.getInstance().levelRenderer;
         Frustum frustum = accessor.create$getCapturedFrustum() != null ? accessor.create$getCapturedFrustum() : accessor.create$getCullingFrustum();
         AABB itemBB = new AABB(itemPos.x - 0.25, itemPos.y - 0.25, itemPos.z - 0.25, itemPos.x + 0.25, itemPos.y + 0.25, itemPos.z + 0.25);
         return !frustum.isVisible(itemBB);
      }
   }

   @NotNull
   public AABB getRenderBoundingBox(@NotNull T blockEntity) {
      return blockEntity instanceof CachedRenderBBBlockEntity cbe ? cbe.getRenderBoundingBox() : super.getRenderBoundingBox(blockEntity);
   }
}
