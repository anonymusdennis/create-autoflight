package net.createmod.catnip.gui.element;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.model.baked.SinglePosVirtualBlockGetter;
import javax.annotation.Nullable;
import net.createmod.catnip.client.render.model.BakedModelBufferer;
import net.createmod.catnip.gui.ILightingSettings;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.impl.client.render.ColoringVertexConsumer;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipClientServices;
import net.createmod.ponder.mixin.client.accessor.ItemRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;

public class GuiGameElement {
   public static GuiGameElement.GuiRenderBuilder of(ItemStack stack) {
      return new GuiGameElement.GuiItemRenderBuilder(stack);
   }

   public static GuiGameElement.GuiRenderBuilder of(ItemLike itemProvider) {
      return new GuiGameElement.GuiItemRenderBuilder(itemProvider);
   }

   public static GuiGameElement.GuiRenderBuilder of(BlockState state) {
      return new GuiGameElement.GuiBlockStateRenderBuilder(state);
   }

   public static GuiGameElement.GuiRenderBuilder of(BlockState state, @Nullable BlockEntity blockEntity) {
      return new GuiGameElement.GuiBlockEntityRenderBuilder(state, blockEntity);
   }

   public static GuiGameElement.GuiRenderBuilder of(BlockEntity blockEntity) {
      return of(blockEntity.getBlockState(), blockEntity);
   }

   public static GuiGameElement.GuiRenderBuilder of(Fluid fluid) {
      return new GuiGameElement.GuiBlockStateRenderBuilder((BlockState)fluid.defaultFluidState().createLegacyBlock().setValue(LiquidBlock.LEVEL, 0));
   }

   public static GuiGameElement.GuiRenderBuilder of(PartialModel partial) {
      return new GuiGameElement.GuiBlockPartialRenderBuilder(partial);
   }

   public static class GuiBlockEntityRenderBuilder extends GuiGameElement.GuiBlockModelRenderBuilder {
      public GuiBlockEntityRenderBuilder(BlockState blockState, @Nullable BlockEntity blockEntity) {
         super(Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState), blockState, blockEntity);
      }

      @Override
      protected void renderModel(BlockRenderDispatcher blockRenderer, BufferSource buffer, PoseStack ms) {
         this.renderBlockEntity(blockRenderer, buffer, ms);
         super.renderModel(blockRenderer, buffer, ms);
      }

      private void renderBlockEntity(BlockRenderDispatcher blockRenderer, BufferSource buffer, PoseStack ms) {
         if (this.blockEntity != null) {
            BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(this.blockEntity);
            if (renderer != null) {
               BlockState stateBefore = this.blockEntity.getBlockState();
               this.blockEntity.setBlockState(this.blockState);
               renderer.render(this.blockEntity, 0.0F, ms, buffer, 15728880, OverlayTexture.NO_OVERLAY);
               this.blockEntity.setBlockState(stateBefore);
            }
         }
      }
   }

   protected static class GuiBlockModelRenderBuilder extends GuiGameElement.GuiRenderBuilder {
      protected BakedModel blockModel;
      protected BlockState blockState;
      @Nullable
      protected BlockEntity blockEntity;

      public GuiBlockModelRenderBuilder(BakedModel blockmodel, @Nullable BlockState blockState, @Nullable BlockEntity blockEntity) {
         this.blockState = blockState == null ? Blocks.AIR.defaultBlockState() : blockState;
         this.blockModel = blockmodel;
         this.blockEntity = blockEntity;
      }

      @Override
      public void render(GuiGraphics graphics) {
         PoseStack poseStack = graphics.pose();
         this.prepareMatrix(poseStack);
         Minecraft mc = Minecraft.getInstance();
         BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
         BufferSource buffer = graphics.bufferSource();
         this.transformMatrix(poseStack);
         RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
         this.renderModel(blockRenderer, buffer, poseStack);
         this.cleanUpMatrix(poseStack);
      }

      protected void renderModel(BlockRenderDispatcher blockRenderer, BufferSource buffer, PoseStack ms) {
         SinglePosVirtualBlockGetter level = SinglePosVirtualBlockGetter.createFullBright();
         level.blockState(this.blockState);
         level.blockEntity(this.blockEntity);
         BakedModelBufferer.bufferModel(
            this.blockModel,
            BlockPos.ZERO,
            level,
            this.blockState,
            ms,
            (layer, shade) -> {
               layer = layer == RenderType.translucent() ? Sheets.translucentCullBlockSheet() : Sheets.cutoutBlockSheet();
               return new ColoringVertexConsumer(
                  buffer.getBuffer(layer),
                  (float)ARGB32.red(this.color) / 255.0F,
                  (float)ARGB32.green(this.color) / 255.0F,
                  (float)ARGB32.blue(this.color) / 255.0F,
                  1.0F
               );
            }
         );
         buffer.endBatch();
      }
   }

   public static class GuiBlockPartialRenderBuilder extends GuiGameElement.GuiBlockModelRenderBuilder {
      public GuiBlockPartialRenderBuilder(PartialModel partial) {
         super(partial.get(), null, null);
      }
   }

   public static class GuiBlockStateRenderBuilder extends GuiGameElement.GuiBlockModelRenderBuilder {
      public GuiBlockStateRenderBuilder(BlockState blockstate) {
         super(Minecraft.getInstance().getBlockRenderer().getBlockModel(blockstate), blockstate, null);
      }

      @Override
      protected void renderModel(BlockRenderDispatcher blockRenderer, BufferSource buffer, PoseStack poseStack) {
         if (this.blockState.getBlock() instanceof BaseFireBlock) {
            Lighting.setupForFlatItems();
            super.renderModel(blockRenderer, buffer, poseStack);
            Lighting.setupFor3DItems();
         } else {
            super.renderModel(blockRenderer, buffer, poseStack);
            if (!this.blockState.getFluidState().isEmpty()) {
               CatnipClientServices.CLIENT_HOOKS.renderFullFluidState(poseStack, buffer, this.blockState.getFluidState());
               buffer.endBatch();
            }
         }
      }
   }

   public static class GuiItemRenderBuilder extends GuiGameElement.GuiRenderBuilder {
      private final ItemStack stack;

      public GuiItemRenderBuilder(ItemStack stack) {
         this.stack = stack;
      }

      public GuiItemRenderBuilder(ItemLike provider) {
         this(new ItemStack(provider));
      }

      @Override
      public void render(GuiGraphics graphics) {
         PoseStack poseStack = graphics.pose();
         this.prepareMatrix(poseStack);
         this.transformMatrix(poseStack);
         renderItemIntoGUI(poseStack, this.stack, this.customLighting == null);
         this.cleanUpMatrix(poseStack);
      }

      public static void renderItemIntoGUI(PoseStack poseStack, ItemStack stack, boolean useDefaultLighting) {
         ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
         BakedModel bakedModel = renderer.getModel(stack, null, null, 0);
         ((ItemRendererAccessor)renderer).catnip$getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
         RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
         RenderSystem.enableBlend();
         RenderSystem.enableCull();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         poseStack.pushPose();
         poseStack.translate(0.0F, 0.0F, 100.0F);
         poseStack.translate(8.0F, -8.0F, 0.0F);
         poseStack.scale(16.0F, 16.0F, 16.0F);
         BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
         boolean flatLighting = !bakedModel.usesBlockLight();
         if (useDefaultLighting && flatLighting) {
            Lighting.setupForFlatItems();
         }

         renderer.render(stack, ItemDisplayContext.GUI, false, poseStack, buffer, 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
         RenderSystem.disableDepthTest();
         buffer.endBatch();
         RenderSystem.enableDepthTest();
         if (useDefaultLighting && flatLighting) {
            Lighting.setupFor3DItems();
         }

         poseStack.popPose();
      }
   }

   public abstract static class GuiRenderBuilder extends AbstractRenderElement {
      protected double xLocal;
      protected double yLocal;
      protected double zLocal;
      protected double xRot;
      protected double yRot;
      protected double zRot;
      protected double scale = 1.0;
      protected int color = 16777215;
      protected Vec3 rotationOffset = Vec3.ZERO;
      @Nullable
      protected ILightingSettings customLighting = null;

      public GuiGameElement.GuiRenderBuilder atLocal(double x, double y, double z) {
         this.xLocal = x;
         this.yLocal = y;
         this.zLocal = z;
         return this;
      }

      public GuiGameElement.GuiRenderBuilder rotate(double xRot, double yRot, double zRot) {
         this.xRot = xRot;
         this.yRot = yRot;
         this.zRot = zRot;
         return this;
      }

      public GuiGameElement.GuiRenderBuilder rotateBlock(double xRot, double yRot, double zRot) {
         return this.rotate(xRot, yRot, zRot).withRotationOffset(VecHelper.getCenterOf(BlockPos.ZERO));
      }

      public GuiGameElement.GuiRenderBuilder scale(double scale) {
         this.scale = scale;
         return this;
      }

      public GuiGameElement.GuiRenderBuilder color(int color) {
         this.color = color;
         return this;
      }

      public GuiGameElement.GuiRenderBuilder withRotationOffset(Vec3 offset) {
         this.rotationOffset = offset;
         return this;
      }

      public GuiGameElement.GuiRenderBuilder lighting(ILightingSettings lighting) {
         this.customLighting = lighting;
         return this;
      }

      protected void prepareMatrix(PoseStack poseStack) {
         poseStack.pushPose();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableDepthTest();
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
         this.prepareLighting(poseStack);
      }

      protected void transformMatrix(PoseStack poseStack) {
         poseStack.translate(this.x, this.y, this.z);
         poseStack.scale((float)this.scale, (float)this.scale, (float)this.scale);
         poseStack.translate(this.xLocal, this.yLocal, this.zLocal);
         UIRenderHelper.flipForGuiRender(poseStack);
         poseStack.translate(this.rotationOffset.x, this.rotationOffset.y, this.rotationOffset.z);
         poseStack.mulPose(Axis.ZP.rotationDegrees((float)this.zRot));
         poseStack.mulPose(Axis.XP.rotationDegrees((float)this.xRot));
         poseStack.mulPose(Axis.YP.rotationDegrees((float)this.yRot));
         poseStack.translate(-this.rotationOffset.x, -this.rotationOffset.y, -this.rotationOffset.z);
      }

      protected void cleanUpMatrix(PoseStack poseStack) {
         poseStack.popPose();
         this.cleanUpLighting(poseStack);
      }

      protected void prepareLighting(PoseStack poseStack) {
         if (this.customLighting != null) {
            this.customLighting.applyLighting();
         } else {
            Lighting.setupFor3DItems();
         }
      }

      protected void cleanUpLighting(PoseStack poseStack) {
         if (this.customLighting != null) {
            Lighting.setupFor3DItems();
         }
      }
   }
}
