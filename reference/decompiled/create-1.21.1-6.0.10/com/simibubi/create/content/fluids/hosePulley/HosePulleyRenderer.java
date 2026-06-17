package com.simibubi.create.content.fluids.hosePulley;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.content.contraptions.pulley.AbstractPulleyRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

public class HosePulleyRenderer extends AbstractPulleyRenderer<HosePulleyBlockEntity> {
   public HosePulleyRenderer(Context context) {
      super(context, AllPartialModels.HOSE_HALF, AllPartialModels.HOSE_HALF_MAGNET);
   }

   protected Axis getShaftAxis(HosePulleyBlockEntity be) {
      return ((Direction)be.getBlockState().getValue(HosePulleyBlock.HORIZONTAL_FACING)).getClockWise().getAxis();
   }

   @Override
   protected PartialModel getCoil() {
      return AllPartialModels.HOSE_COIL;
   }

   protected SuperByteBuffer renderRope(HosePulleyBlockEntity be) {
      return CachedBuffers.partial(AllPartialModels.HOSE, be.getBlockState());
   }

   protected SuperByteBuffer renderMagnet(HosePulleyBlockEntity be) {
      return CachedBuffers.partial(AllPartialModels.HOSE_MAGNET, be.getBlockState());
   }

   protected float getOffset(HosePulleyBlockEntity be, float partialTicks) {
      return be.getInterpolatedOffset(partialTicks);
   }

   @Override
   protected SpriteShiftEntry getCoilShift() {
      return AllSpriteShifts.HOSE_PULLEY_COIL;
   }

   protected boolean isRunning(HosePulleyBlockEntity be) {
      return true;
   }
}
