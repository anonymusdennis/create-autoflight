package com.simibubi.create.content.contraptions.pulley;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractPulleyRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
   private PartialModel halfRope;
   private PartialModel halfMagnet;

   public AbstractPulleyRenderer(Context context, PartialModel halfRope, PartialModel halfMagnet) {
      super(context);
      this.halfRope = halfRope;
      this.halfMagnet = halfMagnet;
   }

   public boolean shouldRenderOffScreen(T p_188185_1_) {
      return true;
   }

   @Override
   protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (!VisualizationManager.supportsVisualization(be.getLevel())) {
         super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
         float offset = this.getOffset(be, partialTicks);
         boolean running = this.isRunning(be);
         VertexConsumer vb = buffer.getBuffer(RenderType.solid());
         scrollCoil(this.getRotatedCoil(be), this.getCoilShift(), offset, 1.0F).light(light).renderInto(ms, vb);
         Level world = be.getLevel();
         BlockState blockState = be.getBlockState();
         BlockPos pos = be.getBlockPos();
         SuperByteBuffer halfMagnet = CachedBuffers.partial(this.halfMagnet, blockState);
         SuperByteBuffer halfRope = CachedBuffers.partial(this.halfRope, blockState);
         SuperByteBuffer magnet = this.renderMagnet(be);
         SuperByteBuffer rope = this.renderRope(be);
         if (running || offset == 0.0F) {
            renderAt(world, offset > 0.25F ? magnet : halfMagnet, offset, pos, ms, vb);
         }

         float f = offset % 1.0F;
         if (offset > 0.75F && (f < 0.25F || f > 0.75F)) {
            renderAt(world, halfRope, f > 0.75F ? f - 1.0F : f, pos, ms, vb);
         }

         if (running) {
            for (int i = 0; (float)i < offset - 1.25F; i++) {
               renderAt(world, rope, offset - (float)i - 1.0F, pos, ms, vb);
            }
         }
      }
   }

   public static void renderAt(LevelAccessor world, SuperByteBuffer partial, float offset, BlockPos pulleyPos, PoseStack ms, VertexConsumer buffer) {
      BlockPos actualPos = pulleyPos.below((int)offset);
      int light = LevelRenderer.getLightColor(world, world.getBlockState(actualPos), actualPos);
      ((SuperByteBuffer)partial.translate(0.0F, -offset, 0.0F)).light(light).renderInto(ms, buffer);
   }

   protected abstract Axis getShaftAxis(T var1);

   protected abstract PartialModel getCoil();

   protected abstract SpriteShiftEntry getCoilShift();

   protected abstract SuperByteBuffer renderRope(T var1);

   protected abstract SuperByteBuffer renderMagnet(T var1);

   protected abstract float getOffset(T var1, float var2);

   protected abstract boolean isRunning(T var1);

   @Override
   protected BlockState getRenderedBlockState(T be) {
      return shaft(this.getShaftAxis(be));
   }

   protected SuperByteBuffer getRotatedCoil(T be) {
      BlockState blockState = be.getBlockState();
      return CachedBuffers.partialFacing(this.getCoil(), blockState, Direction.get(AxisDirection.POSITIVE, this.getShaftAxis(be)));
   }

   public static SuperByteBuffer scrollCoil(SuperByteBuffer sbb, SpriteShiftEntry coilShift, float offset, float speedModifier) {
      if (offset == 0.0F) {
         return sbb;
      } else {
         float spriteSize = coilShift.getTarget().getV1() - coilShift.getTarget().getV0();
         offset *= speedModifier / 2.0F;
         double coilScroll = (double)(-(offset + 0.1875F)) - Math.floor((double)((offset + 0.1875F) * -2.0F)) / 2.0;
         return sbb.shiftUVScrolling(coilShift, (float)coilScroll * spriteSize);
      }
   }

   public int getViewDistance() {
      return (Integer)AllConfigs.server().kinetics.maxRopeLength.get();
   }
}
