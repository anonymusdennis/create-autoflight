package dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.index.SimPartialModels;
import java.util.Vector;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class LinkedTypewriterRenderer extends SmartBlockEntityRenderer<LinkedTypewriterBlockEntity> {
   static Vector<LerpedFloat> keys = new Vector<>(14);

   public LinkedTypewriterRenderer(Context context) {
      super(context);
   }

   public static void tick() {
      if (!Minecraft.getInstance().isPaused()) {
         if (LinkedTypewriterInteractionHandler.getMode() != LinkedTypewriterInteractionHandler.Mode.IDLE) {
            for (int i = 0; i < keys.size(); i++) {
               LerpedFloat lerpedFloat = keys.get(i);
               lerpedFloat.chase(LinkedTypewriterInteractionHandler.getPressedKeys().contains(i) ? 1.0 : 0.0, 0.4F, Chaser.EXP);
               lerpedFloat.tickChaser();
            }
         }
      }
   }

   public static void resetKeys() {
      for (LerpedFloat key : keys) {
         key.startWithValue(0.0);
      }
   }

   protected void renderSafe(LinkedTypewriterBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
      BlockState blockState = be.getBlockState();
      Direction facing = (Direction)blockState.getValue(BlockStateProperties.HORIZONTAL_FACING);
      TransformStack<PoseTransformStack> ps = TransformStack.of(ms);
      float pt = AnimationTickHolder.getPartialTicks();
      float s = 0.0625F;
      float b = -0.046875F;
      int index = 0;
      ps.translate(0.5, 0.25, 0.5);
      ps.rotateYDegrees(AngleHelper.horizontalAngle(facing));
      ps.pushPose();
      if (LinkedTypewriterInteractionHandler.getMode() == LinkedTypewriterInteractionHandler.Mode.BIND) {
         int i = (int)Mth.lerp((Mth.sin(AnimationTickHolder.getRenderTime() / 4.0F) + 1.0F) / 2.0F, 5.0F, 15.0F);
         light = i << 20;
      }

      ps.translate(-0.4375F, 0.0625F, 0.125F);
      ps.pushPose();

      for (int i = 0; i < 6; i++) {
         ps.translate(0.125, 0.0, 0.0);
         renderKey(ms, vb, light, pt, blockState, be, -0.046875F, index++, false);
      }

      ms.popPose();
      ps.translate(-0.0625F, -0.0625F, 0.125F);
      ps.pushPose();

      for (int i = 0; i < 7; i++) {
         ps.translate(0.125, 0.0, 0.0);
         renderKey(ms, vb, light, pt, blockState, be, -0.046875F, index++, false);
      }

      ms.popPose();
      ps.translate(0.5F, -0.0625F, 0.125F);
      ps.pushPose();
      renderKey(ms, vb, light, pt, blockState, be, -0.046875F, index, true);
      ms.popPose();
      ms.popPose();
   }

   protected static void renderKey(
      PoseStack ms, VertexConsumer vb, int light, float pt, BlockState blockState, LinkedTypewriterBlockEntity be, float b, int index, boolean isSpacebar
   ) {
      ms.pushPose();
      float depression = 0.0F;
      if (be.checkUser(Minecraft.getInstance().player.getUUID())) {
         depression = b * keys.get(index).getValue(pt);
      }

      ms.translate(0.0F, depression, 0.0F);
      if (!isSpacebar) {
         CachedBuffers.partial(SimPartialModels.LINKED_TYPEWRITER_KEY, blockState).light(light).renderInto(ms, vb);
      } else {
         CachedBuffers.partial(SimPartialModels.LINKED_TYPEWRITER_KEY_SPACEBAR, blockState).light(light).renderInto(ms, vb);
      }

      ms.popPose();
   }

   static {
      for (int i = 0; i < 14; i++) {
         keys.add(LerpedFloat.linear().startWithValue(0.0));
      }
   }
}
