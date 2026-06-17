package dev.simulated_team.simulated.content.blocks.portable_engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Vector3f;

public class PortableEngineRenderer extends KineticBlockEntityRenderer<PortableEngineBlockEntity> {
   public PortableEngineRenderer(Context context) {
      super(context);
   }

   protected static float getHatchOpenProgress(PortableEngineBlockEntity engine, float partialTicks) {
      return Mth.sin(engine.getHatchOpenTime(partialTicks) / 10.0F * (float) (Math.PI / 2));
   }

   protected void renderSafe(PortableEngineBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      BlockState state = this.getRenderedBlockState(be);
      RenderType type = this.getRenderType(be, state);
      renderRotatingBuffer(be, this.getRotatedModel(be, state), ms, buffer.getBuffer(type), light);
      FilteringRenderer.renderOnBlockEntity(be, partialTicks, ms, buffer, light, overlay);
      VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());
      Direction direction = (Direction)be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
      BlockState blockState = be.getBlockState();
      SimPartialModels.EngineParts engineParts = SimPartialModels.ENGINE_PARTS;
      float visualStrength = be.visualStrength.getValue(partialTicks);
      boolean lit = (Boolean)blockState.getValue(RedstoneTorchBlock.LIT);
      this.renderHatch(be, partialTicks, ms, light, blockState, direction, cutout, 255, engineParts, !lit, false);
      this.renderPipes(be, partialTicks, ms, light, blockState, direction, cutout, 255, engineParts, false);
      float hatchOpenProgress = 1.0F - getHatchOpenProgress(be, partialTicks);
      if (visualStrength > 0.0F) {
         VertexConsumer translucent = buffer.getBuffer(RenderType.translucent());
         engineParts = be.isSuperHeated() ? SimPartialModels.ENGINE_PARTS_SUPERHEATED : SimPartialModels.ENGINE_PARTS_HEATED;
         this.renderPipes(be, partialTicks, ms, 15728880, blockState, direction, translucent, (int)(visualStrength * 255.0F), engineParts, true);
      }

      if (lit) {
         VertexConsumer translucent = buffer.getBuffer(RenderType.translucent());
         this.renderHatch(be, partialTicks, ms, 15728880, blockState, direction, translucent, (int)(hatchOpenProgress * 255.0F), engineParts, lit, true);
      }
   }

   private void renderHatch(
      PortableEngineBlockEntity be,
      float partialTicks,
      PoseStack ms,
      int light,
      BlockState blockState,
      Direction direction,
      VertexConsumer consumer,
      int alpha,
      SimPartialModels.EngineParts parts,
      boolean renderInner,
      boolean lit
   ) {
      if (be.isVirtual()) {
         lit = false;
      }

      double hatchPivotY = 0.30625F;
      double hatchPivotZ = 0.23125F;
      float hatchOpenAmount = getHatchOpenProgress(be, partialTicks) * 0.65F;
      SuperByteBuffer hatchBottom = this.rotateToFacing(CachedBuffers.partial(parts.hatchBottom, blockState), direction);
      if (lit) {
         hatchBottom.disableDiffuse();
      }

      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)hatchBottom.translate(0.0, 0.30625F, 0.23125F)).rotate(-hatchOpenAmount, Direction.EAST))
            .translate(-0.0, -0.30625F, -0.23125F))
         .light(light)
         .color(255, 255, 255, alpha)
         .renderInto(ms, consumer);
      SuperByteBuffer hatchTop = this.rotateToFacing(CachedBuffers.partial(parts.hatchTop, blockState), direction);
      if (lit) {
         hatchTop.disableDiffuse();
      }

      hatchTop.light(light).color(255, 255, 255, alpha).renderInto(ms, consumer);
      if (renderInner) {
         SuperByteBuffer mouth = this.rotateToFacing(CachedBuffers.partial(parts.mouth, blockState), direction.getOpposite());
         if (lit) {
            mouth.disableDiffuse();
         }

         mouth.light(light).renderInto(ms, consumer);
      }
   }

   private void renderPipes(
      PortableEngineBlockEntity be,
      float partialTicks,
      PoseStack ms,
      int light,
      BlockState blockState,
      Direction direction,
      VertexConsumer consumer,
      int alpha,
      SimPartialModels.EngineParts parts,
      boolean lit
   ) {
      float renderTime = AnimationTickHolder.getRenderTime(be.getLevel()) / 20.0F;
      double pulseTime = (double)renderTime * 7.0;
      double clipHeight = 0.65;
      float pulseStrength = 0.03F * be.visualStrength.getValue(partialTicks);
      float pipePulseStrength = pulseStrength * 1.1F;
      float pipeScale = (float)(Math.max(Math.sin(pulseTime) + 0.65, 0.0) - 0.65) * pipePulseStrength + 1.0F;
      float outletScale = (float)(Math.max(Math.sin(pulseTime - 1.15) + 0.65, 0.0) - 0.65) * pulseStrength + 1.0F;
      Vector3f outletRotationPointLeft = new Vector3f(2.2F, 10.2F, 11.0F).div(16.0F);
      Vector3f outletRotationPointRight = new Vector3f(13.6F, 10.2F, 11.0F).div(16.0F);
      float outletRotation = (float)Math.toRadians(7.5);
      Vector3f pipeCenterRight = new Vector3f(14.0F, 10.0F, 8.0F).div(16.0F);
      Vector3f pipeCenterLeft = new Vector3f(2.0F, 10.0F, 8.0F).div(16.0F);
      if (be.isVirtual()) {
         lit = false;
      }

      SuperByteBuffer pipeRight = this.rotateToFacing(CachedBuffers.partial(parts.pipeRight, blockState), direction);
      if (lit) {
         pipeRight.disableDiffuse();
      }

      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)pipeRight.translate(pipeCenterRight)).scale(pipeScale)).translateBack(pipeCenterRight))
         .light(light)
         .color(255, 255, 255, alpha)
         .renderInto(ms, consumer);
      SuperByteBuffer outletRight = this.rotateToFacing(CachedBuffers.partial(parts.outletRight, blockState), direction);
      if (lit) {
         outletRight.disableDiffuse();
      }

      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)outletRight.translate(pipeCenterRight))
                        .scale(outletScale))
                     .translateBack(pipeCenterRight))
                  .translate(outletRotationPointRight))
               .rotateY(-outletRotation))
            .translateBack(outletRotationPointRight))
         .light(light)
         .color(255, 255, 255, alpha)
         .renderInto(ms, consumer);
      SuperByteBuffer pipeLeft = this.rotateToFacing(CachedBuffers.partial(parts.pipeLeft, blockState), direction);
      if (lit) {
         pipeLeft.disableDiffuse();
      }

      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)pipeLeft.translate(pipeCenterLeft)).scale(pipeScale)).translateBack(pipeCenterLeft))
         .light(light)
         .color(255, 255, 255, alpha)
         .renderInto(ms, consumer);
      SuperByteBuffer outletLeft = this.rotateToFacing(CachedBuffers.partial(parts.outletLeft, blockState), direction);
      if (lit) {
         outletLeft.disableDiffuse();
      }

      ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)outletLeft.translate(pipeCenterLeft))
                        .scale(outletScale))
                     .translateBack(pipeCenterLeft))
                  .translate(outletRotationPointLeft))
               .rotateY(outletRotation))
            .translateBack(outletRotationPointLeft))
         .light(light)
         .color(255, 255, 255, alpha)
         .renderInto(ms, consumer);
   }

   protected SuperByteBuffer getRotatedModel(PortableEngineBlockEntity te, BlockState state) {
      return CachedBuffers.partialFacing(
         AllPartialModels.SHAFT_HALF, te.getBlockState(), (Direction)te.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
      );
   }

   protected SuperByteBuffer rotateToFacing(SuperByteBuffer buffer, Direction facing) {
      buffer.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle(facing)), Direction.UP);
      return buffer;
   }
}
