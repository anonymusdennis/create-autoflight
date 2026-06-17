package com.simibubi.create.foundation.blockEntity.behaviour;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;
import java.lang.ref.WeakReference;
import net.createmod.catnip.outliner.ChasingAABBOutline;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ValueBox extends ChasingAABBOutline {
   protected Component label;
   public int overrideColor = -1;
   public boolean isPassive;
   protected ValueBoxTransform transform;
   protected WeakReference<LevelAccessor> level;
   protected BlockPos pos;
   protected BlockState blockState;
   protected AllIcons outline = AllIcons.VALUE_BOX_HOVER_4PX;

   public ValueBox(Component label, AABB bb, BlockPos pos) {
      this(label, bb, pos, Minecraft.getInstance().level.getBlockState(pos));
   }

   public ValueBox(Component label, AABB bb, BlockPos pos, BlockState state) {
      super(bb);
      this.label = label;
      this.pos = pos;
      this.blockState = state;
      this.level = new WeakReference<>(Minecraft.getInstance().level);
   }

   public ValueBox transform(ValueBoxTransform transform) {
      this.transform = transform;
      return this;
   }

   public ValueBox wideOutline() {
      this.outline = AllIcons.VALUE_BOX_HOVER_6PX;
      return this;
   }

   public ValueBox passive(boolean passive) {
      this.isPassive = passive;
      return this;
   }

   public ValueBox withColor(int color) {
      this.overrideColor = color;
      return this;
   }

   public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
      boolean hasTransform = this.transform != null;
      if (this.transform instanceof ValueBoxTransform.Sided && this.params.getHighlightedFace() != null) {
         ((ValueBoxTransform.Sided)this.transform).fromSide(this.params.getHighlightedFace());
      }

      LevelAccessor levelAccessor = this.level.get();
      if (!hasTransform || this.transform.shouldRender(levelAccessor, this.pos, this.blockState)) {
         ms.pushPose();
         ms.translate((double)this.pos.getX() - camera.x, (double)this.pos.getY() - camera.y, (double)this.pos.getZ() - camera.z);
         if (hasTransform) {
            this.transform.transform(levelAccessor, this.pos, this.blockState, ms);
         }

         if (!this.isPassive) {
            ms.pushPose();
            ms.scale(-2.01F, -2.01F, 2.01F);
            ms.translate(-0.5, -0.5, -0.03125);
            this.getOutline().render(ms, buffer, 16777215);
            ms.popPose();
         }

         float fontScale = hasTransform ? -this.transform.getFontScale() : -0.015625F;
         ms.scale(fontScale, fontScale, fontScale);
         this.renderContents(ms, buffer);
         ms.popPose();
      }
   }

   public AllIcons getOutline() {
      return this.outline;
   }

   public void renderContents(PoseStack ms, MultiBufferSource buffer) {
   }

   private static void drawString(PoseStack ms, MultiBufferSource buffer, Component text, float x, float y, int color) {
      Minecraft.getInstance().font.drawInBatch(text, x, y, color, false, ms.last().pose(), buffer, DisplayMode.NORMAL, 0, 15728880);
   }

   private static void drawString8x(PoseStack ms, MultiBufferSource buffer, Component text, float x, float y, int color) {
      Minecraft.getInstance().font.drawInBatch8xOutline(text.getVisualOrderText(), x, y, color, -13421773, ms.last().pose(), buffer, 15728880);
   }

   public static class IconValueBox extends ValueBox {
      AllIcons icon;

      public IconValueBox(Component label, INamedIconOptions iconValue, AABB bb, BlockPos pos) {
         super(label, bb, pos);
         this.icon = iconValue.getIcon();
      }

      @Override
      public void renderContents(PoseStack ms, MultiBufferSource buffer) {
         super.renderContents(ms, buffer);
         float scale = 32.0F;
         ms.scale(scale, scale, scale);
         ms.translate(-0.5F, -0.5F, 0.15625F);
         int overrideColor = this.transform.getOverrideColor();
         this.icon.render(ms, buffer, overrideColor != -1 ? overrideColor : 16777215);
      }
   }

   public static class ItemValueBox extends ValueBox {
      ItemStack stack;
      MutableComponent count;

      public ItemValueBox(Component label, AABB bb, BlockPos pos, ItemStack stack, MutableComponent count) {
         super(label, bb, pos);
         this.stack = stack;
         this.count = count;
      }

      @Override
      public AllIcons getOutline() {
         return !this.stack.isEmpty() ? AllIcons.VALUE_BOX_HOVER_6PX : super.getOutline();
      }

      @Override
      public void renderContents(PoseStack ms, MultiBufferSource buffer) {
         super.renderContents(ms, buffer);
         if (this.count != null) {
            Font font = Minecraft.getInstance().font;
            ms.translate(17.5, -5.0, 7.0);
            boolean isFilter = this.stack.getItem() instanceof FilterItem;
            boolean isEmpty = this.stack.isEmpty();
            ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
            BakedModel modelWithOverrides = itemRenderer.getModel(this.stack, null, null, 0);
            boolean blockItem = modelWithOverrides.isGui3d();
            float scale = 1.5F;
            ms.translate((float)(-font.width(this.count)), 0.0F, 0.0F);
            if (isFilter) {
               ms.translate(-5.0F, 8.0F, 0.0F);
            } else if (isEmpty) {
               ms.translate(-15.0, -1.0, -2.75);
               scale = 1.65F;
            } else {
               ms.translate(-7.0F, 10.0F, blockItem ? 10.25F : 0.0F);
            }

            if (this.count.getString().equals("*")) {
               ms.translate(-1.0F, 3.0F, 0.0F);
            }

            ms.scale(scale, scale, scale);
            ValueBox.drawString8x(ms, buffer, this.count, 0.0F, 0.0F, isFilter ? 16777215 : 15592941);
         }
      }
   }

   public static class TextValueBox extends ValueBox {
      Component text;

      public TextValueBox(Component label, AABB bb, BlockPos pos, Component text) {
         super(label, bb, pos);
         this.text = text;
      }

      public TextValueBox(Component label, AABB bb, BlockPos pos, BlockState state, Component text) {
         super(label, bb, pos, state);
         this.text = text;
      }

      @Override
      public void renderContents(PoseStack ms, MultiBufferSource buffer) {
         super.renderContents(ms, buffer);
         Font font = Minecraft.getInstance().font;
         float scale = 3.0F;
         ms.scale(scale, scale, 1.0F);
         ms.translate(-4.0, -3.75, 5.0);
         int stringWidth = font.width(this.text);
         float numberScale = 9.0F / (float)stringWidth;
         boolean singleDigit = stringWidth < 10;
         if (singleDigit) {
            numberScale /= 2.0F;
         }

         float verticalMargin = (float)(stringWidth - 9) / 2.0F;
         ms.scale(numberScale, numberScale, numberScale);
         ms.translate(singleDigit ? (float)(stringWidth / 2) : 0.0F, singleDigit ? -verticalMargin : verticalMargin, 0.0F);
         int overrideColor = this.transform.getOverrideColor();
         if (overrideColor == -1) {
            ValueBox.drawString8x(ms, buffer, this.text, 0.0F, 0.0F, 15592941);
         } else {
            ValueBox.drawString(ms, buffer, this.text, 0.0F, 0.0F, overrideColor);
         }
      }
   }
}
