package dev.ryanhcode.sable.sublevel.render.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.platform.SableSubLevelRenderPlatform;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class VanillaSingleSubLevelRenderData implements SubLevelRenderData {
   private static final RandomSource RANDOM = RandomSource.create();
   private static final SingleBlockSubLevelWrapper LEVEL_WRAPPER = new SingleBlockSubLevelWrapper();
   private static final Matrix4f TRANSFORM = new Matrix4f();
   private static final Vector3d CENTER_OF_ROT = new Vector3d();
   private final ClientSubLevel subLevel;
   private BlockState singleBlockState = null;
   private BlockPos singleBlockPos = null;
   private long singleBlockSeed = 42L;
   private BlockEntity singleBlockEntity = null;
   private boolean singleBlockEntityGlobal = false;

   public VanillaSingleSubLevelRenderData(ClientSubLevel subLevel) {
      this.subLevel = subLevel;
      this.rebuild();
   }

   private <E extends BlockEntity> void handleBlockEntity(@Nullable E blockEntity) {
      if (!Objects.equals(this.singleBlockEntity, blockEntity)) {
         if (blockEntity == null) {
            this.removeBlockEntity();
         } else {
            BlockEntityRenderer<E> blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            if (blockEntityRenderer == null) {
               this.removeBlockEntity();
            } else {
               this.singleBlockEntity = blockEntity;
               this.singleBlockEntityGlobal = blockEntityRenderer.shouldRenderOffScreen(blockEntity);
            }
         }
      }
   }

   private void removeBlockEntity() {
      if (this.singleBlockEntity != null && this.singleBlockEntityGlobal) {
         Minecraft.getInstance().levelRenderer.updateGlobalBlockEntities(Set.of(this.singleBlockEntity), Set.of());
      }

      this.singleBlockEntity = null;
      this.singleBlockEntityGlobal = false;
   }

   public void renderSingleBlock(RenderType layer, VertexConsumer consumer, Matrix4f modelView, double camX, double camY, double camZ) {
      Minecraft client = Minecraft.getInstance();
      if (this.singleBlockState.isAir()) {
         this.rebuild();
      }

      if (this.singleBlockState.getRenderShape() == RenderShape.MODEL) {
         BakedModel bakedModel = client.getBlockRenderer().getBlockModel(this.singleBlockState);
         Pose3dc renderPose = this.subLevel.renderPose();
         Vector3dc renderPos = renderPose.position();
         LEVEL_WRAPPER.setup(this.subLevel.getLevel(), renderPos.x(), renderPos.y(), renderPos.z(), this.singleBlockPos, this.singleBlockState);
         RANDOM.setSeed(this.singleBlockSeed);
         List<RenderType> renderLayers = SableSubLevelRenderPlatform.INSTANCE
            .getRenderLayers(LEVEL_WRAPPER, bakedModel, this.singleBlockState, this.singleBlockPos, RANDOM);
         if (!renderLayers.contains(layer)) {
            LEVEL_WRAPPER.clear();
         } else {
            PoseStack stack = new PoseStack();
            double renderX = renderPos.x();
            double renderY = renderPos.y();
            double renderZ = renderPos.z();
            Quaterniondc renderRot = renderPose.orientation();
            Vector3d renderCOR = renderRot.transform(
               CENTER_OF_ROT.set(renderPose.rotationPoint())
                  .sub((double)this.singleBlockPos.getX(), (double)this.singleBlockPos.getY(), (double)this.singleBlockPos.getZ())
            );
            renderCOR.negate().add(renderX, renderY, renderZ);
            Matrix4f transform = TRANSFORM.identity();
            transform.translate((float)(renderCOR.x() - camX), (float)(renderCOR.y() - camY), (float)(renderCOR.z() - camZ));
            transform.rotate(new Quaternionf(renderRot));
            stack.last().pose().mul(modelView).mul(transform);
            transform.normal(stack.last().normal());
            SableSubLevelRenderPlatform.INSTANCE
               .tesselateBlock(
                  LEVEL_WRAPPER,
                  bakedModel,
                  this.singleBlockState,
                  this.singleBlockPos,
                  stack,
                  consumer,
                  RANDOM,
                  this.singleBlockSeed,
                  OverlayTexture.NO_OVERLAY,
                  layer
               );
            LEVEL_WRAPPER.clear();
         }
      }
   }

   @Nullable
   public BlockEntity getRenderBlockEntity() {
      if (this.singleBlockState.isAir()) {
         this.rebuild();
      }

      return this.singleBlockEntity;
   }

   @Override
   public void rebuild() {
      BoundingBox3ic bounds = this.subLevel.getPlot().getBoundingBox();
      BlockPos pos = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
      BlockState blockState = this.subLevel.getLevel().getBlockState(pos);
      this.singleBlockState = blockState;
      this.singleBlockPos = pos;
      this.singleBlockSeed = blockState.getSeed(pos);
      this.handleBlockEntity(blockState.hasBlockEntity() ? this.subLevel.getLevel().getBlockEntity(pos) : null);
      if (this.singleBlockEntity != null) {
         SableSubLevelRenderPlatform.INSTANCE.tryAddFlywheelVisual(this.singleBlockEntity);
      }
   }

   @Override
   public void compileSections(PrioritizeChunkUpdates chunkUpdates, RenderRegionCache renderRegionCache, Camera camera) {
   }

   @Override
   public int getVisibleSectionCount() {
      return 1;
   }

   @Override
   public ClientSubLevel getSubLevel() {
      return this.subLevel;
   }

   @Override
   public void setDirty(int x, int y, int z, boolean playerChanged) {
      this.rebuild();
   }

   @Override
   public boolean isSectionCompiled(int x, int y, int z) {
      return true;
   }

   @Override
   public void close() {
      this.removeBlockEntity();
   }
}
