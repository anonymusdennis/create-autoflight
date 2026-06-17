package com.simibubi.create.content.equipment.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class BacktankRenderer extends KineticBlockEntityRenderer<BacktankBlockEntity> {
   public BacktankRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(BacktankBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
      BlockState blockState = be.getBlockState();
      SuperByteBuffer cogs = CachedBuffers.partial(getCogsModel(blockState), blockState);
      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)cogs.center())
                     .rotateYDegrees(180.0F + AngleHelper.horizontalAngle((Direction)blockState.getValue(BacktankBlock.HORIZONTAL_FACING))))
                  .uncenter())
               .translate(0.0F, 0.40625F, 0.6875F))
            .rotate(AngleHelper.rad((double)(be.getSpeed() / 4.0F * AnimationTickHolder.getRenderTime(be.getLevel()) % 360.0F)), Direction.EAST))
         .translate(0.0F, -0.40625F, -0.6875F);
      cogs.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
   }

   protected SuperByteBuffer getRotatedModel(BacktankBlockEntity be, BlockState state) {
      return CachedBuffers.partial(getShaftModel(state), state);
   }

   public static PartialModel getCogsModel(BlockState state) {
      return AllBlocks.NETHERITE_BACKTANK.has(state) ? AllPartialModels.NETHERITE_BACKTANK_COGS : AllPartialModels.COPPER_BACKTANK_COGS;
   }

   public static PartialModel getShaftModel(BlockState state) {
      return AllBlocks.NETHERITE_BACKTANK.has(state) ? AllPartialModels.NETHERITE_BACKTANK_SHAFT : AllPartialModels.COPPER_BACKTANK_SHAFT;
   }
}
