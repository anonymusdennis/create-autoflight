package com.simibubi.create.compat.jei.category.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class AnimatedDeployer extends AnimatedKinetics {
   public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
      PoseStack matrixStack = graphics.pose();
      matrixStack.pushPose();
      matrixStack.translate((float)xOffset, (float)yOffset, 100.0F);
      matrixStack.mulPose(Axis.XP.rotationDegrees(-15.5F));
      matrixStack.mulPose(Axis.YP.rotationDegrees(22.5F));
      int scale = 20;
      this.blockElement(this.shaft(net.minecraft.core.Direction.Axis.Z)).rotateBlock(0.0, 0.0, (double)getCurrentAngle()).scale((double)scale).render(graphics);
      this.blockElement(
            (BlockState)((BlockState)AllBlocks.DEPLOYER.getDefaultState().setValue(DeployerBlock.FACING, Direction.DOWN))
               .setValue(DeployerBlock.AXIS_ALONG_FIRST_COORDINATE, false)
         )
         .scale((double)scale)
         .render(graphics);
      float cycle = (AnimationTickHolder.getRenderTime() - (float)(this.offset * 8)) % 30.0F;
      float offset = cycle < 10.0F ? cycle / 10.0F : (cycle < 20.0F ? (20.0F - cycle) / 10.0F : 0.0F);
      matrixStack.pushPose();
      matrixStack.translate(0.0F, offset * 17.0F, 0.0F);
      this.blockElement(AllPartialModels.DEPLOYER_POLE).rotateBlock(90.0, 0.0, 0.0).scale((double)scale).render(graphics);
      this.blockElement(AllPartialModels.DEPLOYER_HAND_HOLDING).rotateBlock(90.0, 0.0, 0.0).scale((double)scale).render(graphics);
      matrixStack.popPose();
      this.blockElement(AllBlocks.DEPOT.getDefaultState()).atLocal(0.0, 2.0, 0.0).scale((double)scale).render(graphics);
      matrixStack.popPose();
   }
}
