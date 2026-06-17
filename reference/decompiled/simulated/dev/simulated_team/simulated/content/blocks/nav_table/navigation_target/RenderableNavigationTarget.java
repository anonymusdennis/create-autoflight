package dev.simulated_team.simulated.content.blocks.nav_table.navigation_target;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface RenderableNavigationTarget extends NavigationTarget {
   default void renderInNavTable(
      ItemStack self, NavTableBlockEntity navBE, BlockState navState, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay
   ) {
      ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
      itemRenderer.renderStatic(self, ItemDisplayContext.FIXED, light, overlay, ms, buffer, navBE.getLevel(), 0);
   }
}
