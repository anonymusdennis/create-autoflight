package com.simibubi.create.content.trains.track;

import com.simibubi.create.foundation.model.BakedQuadHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TrackModel extends BakedModelWrapper<BakedModel> {
   public TrackModel(BakedModel originalModel) {
      super(originalModel);
   }

   @NotNull
   public List<BakedQuad> getQuads(
      @Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType
   ) {
      List<BakedQuad> templateQuads = super.getQuads(state, side, rand, extraData, renderType);
      if (templateQuads.isEmpty()) {
         return templateQuads;
      } else if (!extraData.has(TrackBlockEntityTilt.ASCENDING_PROPERTY)) {
         return templateQuads;
      } else {
         double angleIn = (Double)extraData.get(TrackBlockEntityTilt.ASCENDING_PROPERTY);
         double angle = Math.abs(angleIn);
         boolean flip = angleIn < 0.0;
         TrackShape trackShape = (TrackShape)state.getValue(TrackBlock.SHAPE);

         double hAngle = switch (trackShape) {
            case XO -> 0.0;
            case PD -> 45.0;
            case ZO -> 90.0;
            case ND -> 135.0;
            default -> 0.0;
         };
         Vec3 verticalOffset = new Vec3(0.0, -0.25, 0.0);
         Vec3 diagonalRotationPoint = trackShape != TrackShape.ND && trackShape != TrackShape.PD
            ? Vec3.ZERO
            : new Vec3((double)((Mth.SQRT_OF_TWO - 1.0F) / 2.0F), 0.0, 0.0);
         UnaryOperator<Vec3> transform = v -> {
            v = v.add(verticalOffset);
            v = VecHelper.rotateCentered(v, hAngle, Axis.Y);
            v = v.add(diagonalRotationPoint);
            v = VecHelper.rotate(v, angle, Axis.Z);
            v = v.subtract(diagonalRotationPoint);
            v = VecHelper.rotateCentered(v, -hAngle + (double)(flip ? 180 : 0), Axis.Y);
            return v.subtract(verticalOffset);
         };
         int size = templateQuads.size();
         List<BakedQuad> quads = new ArrayList<>();

         for (BakedQuad templateQuad : templateQuads) {
            BakedQuad quad = BakedQuadHelper.clone(templateQuad);
            int[] vertexData = quad.getVertices();

            for (int j = 0; j < 4; j++) {
               BakedQuadHelper.setXYZ(vertexData, j, transform.apply(BakedQuadHelper.getXYZ(vertexData, j)));
            }

            quads.add(quad);
         }

         return quads;
      }
   }
}
