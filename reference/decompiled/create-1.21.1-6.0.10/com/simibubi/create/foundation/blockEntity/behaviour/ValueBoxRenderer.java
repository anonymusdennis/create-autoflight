package com.simibubi.create.foundation.blockEntity.behaviour;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import org.joml.Matrix3f;

public class ValueBoxRenderer {
   public static void renderItemIntoValueBox(ItemStack filter, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      Minecraft mc = Minecraft.getInstance();
      ItemRenderer itemRenderer = mc.getItemRenderer();
      BakedModel modelWithOverrides = itemRenderer.getModel(filter, null, null, 0);
      boolean blockItem = modelWithOverrides.isGui3d();
      float scale = (!blockItem ? 0.5F : 1.0F) + 0.015625F;
      float zOffset = (!blockItem ? -0.15F : 0.0F) + customZOffset(filter.getItem());
      ms.scale(scale, scale, scale);
      ms.translate(0.0F, 0.0F, zOffset);
      itemRenderer.render(filter, ItemDisplayContext.FIXED, false, ms, buffer, light, overlay, modelWithOverrides);
   }

   public static void renderFlatItemIntoValueBox(ItemStack filter, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!filter.isEmpty()) {
         int bl = light >> 4 & 15;
         int sl = light >> 20 & 15;
         int itemLight = Mth.floor((double)sl + 0.5) << 20 | (Mth.floor((double)bl + 0.5) & 15) << 4;
         ms.pushPose();
         TransformStack.of(ms).rotateXDegrees(230.0F);
         Matrix3f copy = new Matrix3f(ms.last().normal());
         ms.popPose();
         ms.pushPose();
         ((PoseTransformStack)TransformStack.of(ms).translate(0.0F, 0.0F, -0.25F).translate(0.0, 0.0, 0.03225)).rotateYDegrees(180.0F);
         PoseStack squashedMS = new PoseStack();
         squashedMS.last().pose().mul(ms.last().pose());
         squashedMS.scale(0.5F, 0.5F, 9.765625E-4F);
         squashedMS.last().normal().set(copy);
         Minecraft mc = Minecraft.getInstance();
         mc.getItemRenderer().renderStatic(filter, ItemDisplayContext.GUI, itemLight, OverlayTexture.NO_OVERLAY, squashedMS, buffer, mc.level, 0);
         ms.popPose();
      }
   }

   private static float customZOffset(Item item) {
      float nudge = -0.1F;
      if (item instanceof BlockItem) {
         Block block = ((BlockItem)item).getBlock();
         if (block instanceof AbstractSimpleShaftBlock) {
            return nudge;
         }

         if (block instanceof FenceBlock) {
            return nudge;
         }

         if (block.builtInRegistryHolder().is(BlockTags.BUTTONS)) {
            return nudge;
         }

         if (block == Blocks.END_ROD) {
            return nudge;
         }
      }

      return 0.0F;
   }
}
