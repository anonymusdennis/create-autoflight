package dev.eriksonn.aeronautics.content.blocks.propeller.small;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public abstract class SimplePropellerRenderer<T extends BasePropellerBlockEntity> extends KineticBlockEntityRenderer<T> {
   public SimplePropellerRenderer(Context context) {
      super(context);
   }

   public void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         BlockState state = be.getBlockState();
         Direction dir = (Direction)state.getValue(BlockStateProperties.FACING);
         VertexConsumer vb = buffer.getBuffer(RenderType.solid());
         SuperByteBuffer propeller = CachedBuffers.partialFacing(this.getCurrentModel(be), state);
         float angle = this.getAngle(partialTicks, dir, be);
         kineticRotationTransform(propeller, be, dir.getAxis(), angle, light);
         if (dir.getAxis().isHorizontal()) {
            propeller.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(dir.getOpposite())), Direction.UP);
         }

         if (dir.getAxis().isVertical()) {
            propeller.rotateCentered(AngleHelper.rad((double)AngleHelper.verticalAngle(dir.getOpposite())), Direction.EAST);
         }

         ((SuperByteBuffer)propeller.translate(0.0F, 0.0F, -0.1875F))
            .rotateCentered(AngleHelper.rad((double)(-90.0F - AngleHelper.verticalAngle(dir))), Direction.EAST);
         propeller.renderInto(ms, vb);
      }
   }

   public abstract PartialModel getCurrentModel(T var1);

   public float getAngle(float partialTicks, Direction dir, T be) {
      float angle = be.getPreviousAngle() * (1.0F - partialTicks) + be.getAngle() * partialTicks;
      angle = angle / 180.0F * (float) Math.PI;
      return angle * 2.0F;
   }

   protected SuperByteBuffer getRotatedModel(T be, BlockState state) {
      return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, ((Direction)state.getValue(BearingBlock.FACING)).getOpposite());
   }
}
