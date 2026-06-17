package dev.eriksonn.aeronautics.content.blocks.propeller.small.smart_propeller;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.SimplePropellerRenderer;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SmartPropellerRenderer extends SimplePropellerRenderer<SmartPropellerBlockEntity> {
   public SmartPropellerRenderer(Context context) {
      super(context);
   }

   public void renderSafe(SmartPropellerBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState state = this.getRenderedBlockState(be);
      RenderType type = this.getRenderType(be, state);
      renderRotatingBuffer(be, this.getRotatedModel(be, state), ms, buffer.getBuffer(type), light);
      Axis horizontal = (Axis)state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
      VertexConsumer vb = buffer.getBuffer(RenderType.solid());
      SuperByteBuffer propeller = CachedBuffers.partialFacing(this.getCurrentModel(be), state, Direction.UP).light(light);
      SuperByteBuffer hinge = CachedBuffers.partialFacing(AeroPartialModels.SMART_PROPELLER_HINGE, state, Direction.UP).light(light);
      float hingeAngle = be.getLerpedHingeAngle(partialTicks);
      float angle = this.getAngle(partialTicks, Direction.UP, be);
      Direction d = Direction.get(AxisDirection.NEGATIVE, horizontal);
      hinge.rotateCentered(AngleHelper.rad((double)hingeAngle), d.getClockWise());
      propeller.rotateCentered(AngleHelper.rad((double)hingeAngle), d.getClockWise());
      float factChecked = AngleHelper.rad((double)AngleHelper.horizontalAngle(d));
      propeller.rotateCentered(factChecked, Direction.UP);
      hinge.rotateCentered(factChecked, Direction.UP);
      kineticRotationTransform(propeller, be, Direction.UP.getAxis(), angle, light);
      propeller.translate(0.0F, 0.625F, 0.0F);
      propeller.rotateCentered(AngleHelper.rad(90.0), Direction.EAST);
      hinge.translate(0.0F, -0.0625F, 0.0F);
      hinge.rotateCentered(AngleHelper.rad(90.0), Direction.EAST);
      propeller.renderInto(ms, vb);
      hinge.renderInto(ms, vb);
   }

   public PartialModel getCurrentModel(SmartPropellerBlockEntity be) {
      return be.getBlockState().getValue(SmartPropellerBlock.REVERSED) ? AeroPartialModels.SMART_PROPELLER_REVERSED : AeroPartialModels.SMART_PROPELLER;
   }

   protected SuperByteBuffer getRotatedModel(SmartPropellerBlockEntity be, BlockState state) {
      return CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, Direction.DOWN);
   }
}
