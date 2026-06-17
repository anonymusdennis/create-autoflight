package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class ModulatingLinkedReceiverRenderer extends SmartBlockEntityRenderer<ModulatingLinkedReceiverBlockEntity> {
   public ModulatingLinkedReceiverRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(ModulatingLinkedReceiverBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource bufferSource, int light, int overlay) {
      super.renderSafe(be, partialTicks, ms, bufferSource, light, overlay);
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, bufferSource, light, overlay);
         Direction facing = (Direction)be.getBlockState().getValue(BlockStateProperties.FACING);
         Vec3 pixelNormal = new Vec3(facing.step()).scale(0.0625);
         float minPos = 5.5F * (float)(be.minRange - 1) * 275.0F / (255.0F * (20.0F + (float)be.minRange - 1.0F));
         float maxPos = 5.5F * (float)(be.maxRange - 1) * 275.0F / (255.0F * (20.0F + (float)be.maxRange - 1.0F));

         for (boolean bottom : Iterate.trueAndFalse) {
            SuperByteBuffer superBuffer = CachedBuffers.partial(SimPartialModels.MODULATING_RECEIVER_PLATE, be.getBlockState());
            if (bottom) {
               superBuffer.translate(pixelNormal.scale((double)minPos));
            } else {
               superBuffer.translate(pixelNormal.scale(0.5 + (double)maxPos));
            }

            if (facing.getAxis().isHorizontal()) {
               superBuffer.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing.getOpposite())), Direction.UP);
            }

            superBuffer.rotateCentered(AngleHelper.rad((double)(-90.0F - AngleHelper.verticalAngle(facing))), Direction.EAST);
            superBuffer.light(light);
            superBuffer.renderInto(ms, bufferSource.getBuffer(RenderType.solid()));
         }
      }
   }
}
