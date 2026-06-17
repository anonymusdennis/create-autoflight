package net.createmod.catnip.ghostblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.model.baked.EmptyVirtualBlockGetter;
import net.createmod.catnip.client.render.model.BakedModelBufferer;
import net.createmod.catnip.impl.client.render.ColoringVertexConsumer;
import net.createmod.catnip.placement.PlacementClient;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public abstract class GhostBlockRenderer {
   private static final GhostBlockRenderer STANDARD = new GhostBlockRenderer.DefaultGhostBlockRenderer();
   private static final GhostBlockRenderer TRANSPARENT = new GhostBlockRenderer.TransparentGhostBlockRenderer();

   public static GhostBlockRenderer standard() {
      return STANDARD;
   }

   public static GhostBlockRenderer transparent() {
      return TRANSPARENT;
   }

   public abstract void render(PoseStack var1, SuperRenderTypeBuffer var2, Vec3 var3, GhostBlockParams var4);

   private static class DefaultGhostBlockRenderer extends GhostBlockRenderer {
      @Override
      public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, GhostBlockParams params) {
         BlockState state = params.state;
         BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
         BlockPos pos = params.pos;
         ms.pushPose();
         ms.translate((double)pos.getX() - camera.x, (double)pos.getY() - camera.y, (double)pos.getZ() - camera.z);
         BakedModelBufferer.bufferModel(model, pos, EmptyVirtualBlockGetter.FULL_BRIGHT, state, ms, (layer, shade) -> buffer.getEarlyBuffer(layer));
         ms.popPose();
      }
   }

   private static class TransparentGhostBlockRenderer extends GhostBlockRenderer {
      @Override
      public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, GhostBlockParams params) {
         BlockState state = params.state;
         BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
         BlockPos pos = params.pos;
         float alpha = params.alphaSupplier.get() * 0.75F * PlacementClient.getCurrentAlpha();
         VertexConsumer vb = new ColoringVertexConsumer(buffer.getEarlyBuffer(RenderType.translucent()), 1.0F, 1.0F, 1.0F, alpha);
         ms.pushPose();
         ms.translate((double)pos.getX() - camera.x, (double)pos.getY() - camera.y, (double)pos.getZ() - camera.z);
         ms.translate(0.5, 0.5, 0.5);
         ms.scale(0.85F, 0.85F, 0.85F);
         ms.translate(-0.5, -0.5, -0.5);
         BakedModelBufferer.bufferModel(model, pos, EmptyVirtualBlockGetter.FULL_BRIGHT, state, ms, (layer, shade) -> vb);
         ms.popPose();
      }
   }
}
