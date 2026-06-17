package dev.simulated_team.simulated.content.blocks.nav_table;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.RenderableNavigationTarget;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.util.SimColors;
import dev.simulated_team.simulated.util.SimDirectionUtil;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class NavTableRenderer extends SmartBlockEntityRenderer<NavTableBlockEntity> {
   public NavTableRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(NavTableBlockEntity navBE, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(navBE, partialTicks, ms, buffer, light, overlay);
      ItemStack heldItem = navBE.getHeldItem();
      BlockState navState = navBE.getBlockState();
      Direction facing = (Direction)navState.getValue(NavTableBlock.FACING);
      ((PoseTransformStack)TransformStack.of(ms).pushPose().center()).rotate(facing.getRotation());
      float arrowAngle = (float)((double)navBE.getClientTargetAngle(partialTicks) - (Math.PI / 2));
      if (!VisualizationManager.supportsVisualization(navBE.getLevel())) {
         ms.pushPose();
         ms.translate(0.0, -0.5, 0.0);
         Vector3f logicalDirectionF = new Vector3f();

         for (Direction direction : SimDirectionUtil.Y_AXIS_PLANE) {
            facing.getRotation().transform((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ(), logicalDirectionF);
            Direction logicalDirection = Direction.getNearest(logicalDirectionF.x, logicalDirectionF.y, logicalDirectionF.z);
            ms.pushPose();
            SuperByteBuffer indicator = CachedBuffers.partial(SimPartialModels.NAV_TABLE_INDICATOR, navState);
            indicator.rotateToFace(direction);
            indicator.translate(0.0, 0.0, 0.5);
            float signalStrength = navBE.isPowering ? (float)Math.max(navBE.getRedstoneStrength(logicalDirection), 0) / 15.0F : 0.0F;
            int color = SimColors.redstone(signalStrength);
            indicator.light(light).color(color).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
            ms.popPose();
         }

         ms.popPose();
         ms.pushPose();
         ms.translate(0.0, 0.3, 0.0);
         SuperByteBuffer pointer = CachedBuffers.partial(SimPartialModels.NAV_TABLE_POINTER, navState);
         pointer.rotateY(arrowAngle);
         pointer.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
         ms.popPose();
      }

      ms.pushPose();
      ms.translate(0.0, 0.3, 0.0);
      ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
      boolean blockItem = itemRenderer.getModel(heldItem, null, null, 0).isGui3d();
      ((PoseTransformStack)TransformStack.of(ms).translate(0.0F, blockItem ? 0.25F : 0.15F, 0.0F).rotate((float)Math.toRadians(90.0), Direction.WEST))
         .scale(blockItem ? 0.5F : 0.375F);
      if (heldItem.getItem() instanceof RenderableNavigationTarget rnti) {
         rnti.renderInNavTable(heldItem, navBE, navState, partialTicks, ms, buffer, light, overlay);
      } else {
         if (heldItem.is(SimTags.Items.ROTATE_WITH_NAV_ARROW)) {
            ms.mulPose(Axis.ZP.rotation(arrowAngle));
         }

         itemRenderer.renderStatic(heldItem, ItemDisplayContext.FIXED, light, overlay, ms, buffer, navBE.getLevel(), 0);
      }

      ms.popPose();
      ms.popPose();
   }
}
